import tensorflow as tf

# Read data
BUCKET = None
PATTERN = "*"

CSV_COLUMNS = ["dayofweek", "hourofday", "pickup_borough", "dropoff_borough", "trip_duration"]
LABEL_COLUMN = "trip_duration"

# Set default values for each CSV column
DEFAULTS = [[1], [0], [""], [""],  []]

# Define some hyperparameters
TRAIN_STEPS = 10000
EVAL_STEPS = None
BATCH_SIZE = 512
NEMBEDS = 3
NNSIZE = [64, 16, 4]

TROTTLE_SECS = None
EVAL_DELAY_SECS = None

VERBOSITY = 'INFO'

tf.logging.set_verbosity(v=VERBOSITY)


# Create an input function reading a file using the Dataset API
# Then provide the results to the Estimator API
def read_dataset(prefix, mode, batch_size):
    def _input_fn():
        def decode_csv(records):
            columns = tf.decode_csv(records, record_defaults=DEFAULTS)
            features = dict(zip(CSV_COLUMNS, columns))
            features["dayofweek"] -= 1
            label = features.pop(LABEL_COLUMN)
            return features, label

        # filename
        file_name = '{}/{}/tlc_yellow_trips_2018-{}.csv'.format(BUCKET, prefix, PATTERN)
        
        # Create list of files that match pattern
        file_list = tf.gfile.Glob(file_name)

        # Create dataset from file list
        dataset = (tf.data.TextLineDataset(file_list, compression_type="GZIP")  # Read text file
                   .map(decode_csv))  # Transform each elem by applying decode_csv fn

        if mode == tf.estimator.ModeKeys.TRAIN:
            num_epochs = None  # indefinitely
            dataset = dataset.shuffle(buffer_size=10*batch_size, seed=1234)
        else:
            num_epochs = 1  # end-of-input after this

        dataset = dataset.repeat(num_epochs).batch(batch_size)
        return dataset
    return _input_fn


# Feature engineering
def get_wide_deep():
    
    borough_list = ["Manhattan", "Queens", "Brooklyn", "Bronx", "Staten Island", "EWR"]
        
    # One hot encode categorical features
    fc_dayofweek = tf.feature_column.categorical_column_with_identity(key="dayofweek", num_buckets=7)
    fc_hourofday = tf.feature_column.categorical_column_with_identity(key="hourofday", num_buckets=24)
    fc_pickuploc = tf.feature_column.categorical_column_with_vocabulary_list(key="pickup_borough", 
                                                                             vocabulary_list=borough_list)
    fc_dropoffloc = tf.feature_column.categorical_column_with_vocabulary_list(key="dropoff_borough", 
                                                                              vocabulary_list=borough_list)
    
    # Cross features to get combination of day and hour and pickup-dropoff locations
    fc_crossed_day_hr = tf.feature_column.crossed_column(keys=[fc_dayofweek, fc_hourofday], hash_bucket_size=24*7)
    fc_crossed_pd_pair = tf.feature_column.crossed_column(keys=[fc_pickuploc, fc_dropoffloc], hash_bucket_size=6*6)
    
    wide = [
        # Feature crosses
        fc_crossed_day_hr, fc_crossed_pd_pair,
        
        # Sparse columns
        fc_dayofweek, fc_hourofday,
        fc_pickuploc, fc_dropoffloc
    ]
    
    # Embedding_column to "group" together ...
    fc_embed_pd_pair = tf.feature_column.embedding_column(categorical_column=fc_crossed_pd_pair, dimension=NEMBEDS)
    fc_embed_day_hr = tf.feature_column.embedding_column(categorical_column=fc_crossed_day_hr, dimension=NEMBEDS)
    
    deep = [
        fc_embed_pd_pair,
        fc_embed_day_hr
    ]
    
    return wide, deep


# Serving input receiver function
def serving_input_receiver_fn():
    receiver_tensors = {
        'dayofweek': tf.placeholder(dtype=tf.int64, shape=[None], name="dayofweek"),
        'hourofday': tf.placeholder(dtype=tf.int64, shape=[None], name="hourofday"),
        'pickup_borough': tf.placeholder(dtype=tf.string, shape=[None], name="pickup_borough"),
        'dropoff_borough': tf.placeholder(dtype=tf.string, shape=[None], name="dropoff_borough"),
    }
    
    features = {
        key: tf.expand_dims(tensor, -1)
        for key, tensor in receiver_tensors.items()
    }
        
    return tf.estimator.export.ServingInputReceiver(features = features, receiver_tensors = receiver_tensors)


# Build and train model
# Create estimator to train and evaluate
def train_and_evaluate(output_dir):
    tf.summary.FileWriterCache.clear() # ensure filewriter cache is clear for TensorBoard events file

    wide, deep = get_wide_deep()
    
    run_config = tf.estimator.RunConfig(save_checkpoints_secs=TROTTLE_SECS,
                                        tf_random_seed=2810,
                                        keep_checkpoint_max=3)
    
    # Add custom evaluation metric
    def my_rmse(labels, predictions):
        pred_values = tf.squeeze(input=predictions["predictions"], axis=-1)
        return {"rmse": tf.metrics.root_mean_squared_error(labels=labels, predictions=pred_values)}
    
    estimator = tf.estimator.DNNLinearCombinedRegressor(
        model_dir=output_dir,
        linear_feature_columns=wide,
        dnn_feature_columns=deep,
        dnn_hidden_units=NNSIZE,
        config=run_config)
    
    estimator = tf.contrib.estimator.add_metrics(estimator=estimator, metric_fn=my_rmse)
    
    train_spec = tf.estimator.TrainSpec(
        input_fn=read_dataset('train', tf.estimator.ModeKeys.TRAIN, BATCH_SIZE),
        max_steps=TRAIN_STEPS)
    
    exporter = tf.estimator.LatestExporter('exporter', serving_input_receiver_fn=serving_input_receiver_fn)
    
    eval_spec = tf.estimator.EvalSpec(
        input_fn=read_dataset('val', tf.estimator.ModeKeys.EVAL, 2**15),
        steps=EVAL_STEPS,
        start_delay_secs=EVAL_DELAY_SECS,  # start evaluating after N seconds
        throttle_secs=TROTTLE_SECS,  # evaluate every N seconds
        exporters=exporter)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)
