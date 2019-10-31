import tensorflow as tf
import numpy as np
import shutil
print(tf.__version__)

# Read data

CSV_COLUMNS = ["dayofweek", "hourofday", "pickup_borough", "dropoff_borough", "trip_duration"]
LABEL_COLUMN = "trip_duration"
DEFAULTS = [[1], [0], [""], [""],  []]

# Create an input function reading a file using the Dataset API
# Then provide the results to the Estimator API
def read_dataset(filename, mode, batch_size = 512):
    def _input_fn():
        def decode_csv(records):
            columns = tf.decode_csv(records, record_defaults=DEFAULTS)
            features = dict(zip(CSV_COLUMNS, columns))
            features["dayofweek"] = features["dayofweek"] - 1
            label = features.pop(LABEL_COLUMN)
            return features, label
        
        # Create list of files that match pattern
        file_list = tf.gfile.Glob(filename)

        # Create dataset from file list
        dataset = (tf.data.TextLineDataset(file_list,compression_type="GZIP")  # Read text file
                   .map(decode_csv))  # Transform each elem by applying decode_csv fn

        if mode == tf.estimator.ModeKeys.TRAIN:
            num_epochs = 5 # None # indefinitely
            dataset = dataset.shuffle(buffer_size=10*batch_size, seed=42)
        else:
            num_epochs = 1 # end-of-input after this

        dataset = dataset.repeat(num_epochs).batch(batch_size)
        return dataset
    return _input_fn

def get_train_input_fn():
    return read_dataset('gs://edml/data/taxi-trips/train/tlc_yellow_trips_2018-000*.csv',
                        mode = tf.estimator.ModeKeys.TRAIN)

def get_valid_input_fn():
    return read_dataset('gs://edml/data/taxi-trips/val/tlc_yellow_trips_2018-000*.csv',
                        mode = tf.estimator.ModeKeys.EVAL)

# Feature engineering

def get_wide_deep():
    
    borough_list = ["Manhattan", "Queens", "Brooklyn", "Bronx", "Staten Island", "EWR"]
        
    # One hot encode categorical features
    fc_dayofweek = tf.feature_column.categorical_column_with_identity(key="dayofweek", num_buckets = 7)
    fc_hourofday = tf.feature_column.categorical_column_with_identity(key="hourofday", num_buckets = 24)
    fc_pickuploc = tf.feature_column.categorical_column_with_vocabulary_list(key="pickup_borough", 
                                                                             vocabulary_list=borough_list)
    fc_dropoffloc = tf.feature_column.categorical_column_with_vocabulary_list(key="dropoff_borough", 
                                                                              vocabulary_list=borough_list)
    
    # Cross features to get combination of day and hour and pickup-dropoff locations
    fc_crossed_day_hr = tf.feature_column.crossed_column(keys = [fc_dayofweek, fc_hourofday], hash_bucket_size = 24 * 7)
    fc_crossed_pd_pair = tf.feature_column.crossed_column(keys = [fc_pickuploc, fc_dropoffloc], hash_bucket_size = 6*6)
    
    wide = [
        # Feature crosses
        fc_crossed_day_hr, fc_crossed_pd_pair,
        
        # Sparse columns
        fc_dayofweek, fc_hourofday,
        fc_pickuploc, fc_dropoffloc
    ]
    
    # Embedding_column to "group" together ...
    fc_embed_pd_pair = tf.feature_column.embedding_column(categorical_column = fc_crossed_pd_pair, dimension = 4)
    fc_embed_day_hr = tf.feature_column.embedding_column(categorical_column = fc_crossed_day_hr, dimension = 16)
    
    deep = [
        fc_embed_pd_pair,
        fc_embed_day_hr
    ]
    
    return wide, deep

# Serving input reciver function

def serving_input_receiver_fn():
    receiver_tensors = {
        'dayofweek' : tf.placeholder(dtype = tf.int64, shape = [None], name="dayofweek"),
        'hourofday' : tf.placeholder(dtype = tf.int64, shape = [None], name="hourofday"),
        'pickup_borough' : tf.placeholder(dtype = tf.string, shape = [None], name="pickup_borough"), 
        'dropoff_borough' : tf.placeholder(dtype = tf.string, shape = [None], name="dropoff_borough"),
    }
    
    features = {
        key: tf.expand_dims(tensor, -1)
        for key, tensor in receiver_tensors.items()
    }
        
    return tf.estimator.export.ServingInputReceiver(features = features, receiver_tensors = receiver_tensors)

# Build and train model

# Create estimator to train and evaluate
def train_and_evaluate(output_dir):
    
    EVAL_INTERVAL = 300
    wide, deep = get_wide_deep()
    
    run_config = tf.estimator.RunConfig(save_checkpoints_secs = EVAL_INTERVAL,
                                        tf_random_seed = 2810,
                                        keep_checkpoint_max = 3)
    
    # Add custom evaluation metric
    def my_rmse(labels, predictions):
        pred_values = tf.squeeze(input = predictions["predictions"], axis = -1)
        return {"rmse": tf.metrics.root_mean_squared_error(labels = labels, predictions = pred_values)}
    
    estimator = tf.estimator.DNNLinearCombinedRegressor(
        model_dir = output_dir,
        linear_feature_columns = wide,
        dnn_feature_columns = deep,
        dnn_hidden_units = [128, 64, 32],
        config = run_config)
    
    estimator = tf.contrib.estimator.add_metrics(estimator = estimator, metric_fn = my_rmse) 
    
    train_spec = tf.estimator.TrainSpec(
        input_fn = get_train_input_fn(),
        max_steps = 500)
    
    exporter = tf.estimator.LatestExporter('exporter', serving_input_receiver_fn = serving_input_receiver_fn)
    
    eval_spec = tf.estimator.EvalSpec(
        input_fn = get_valid_input_fn(),
        steps = None,
        start_delay_secs = 60, # start evaluating after N seconds
        throttle_secs = EVAL_INTERVAL,  # evaluate every N seconds
        exporters = exporter)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)
    
# Train model

OUTDIR = "gs://edml/data/taxi-trips/model"
# gsutil rm gs://edml/data/taxi-trips/model** # start fresh each time
tf.summary.FileWriterCache.clear() # ensure filewriter cache is clear for TensorBoard events file
tf.logging.set_verbosity(v = tf.logging.INFO) # so loss is printed during training

