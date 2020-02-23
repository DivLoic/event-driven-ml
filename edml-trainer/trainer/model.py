import tensorflow as tf
import tensorflow_io as tfio
from tensorflow_io.bigquery import BigQueryClient

from . import util

PROJECT_ID = "event-driven-ml"
DATASET_GCP_PROJECT_ID = "event-driven-ml"
DATASET_ID = "new_york_taxi_trips"
TABLE_ID = "tlc_yellow_trips_2018"

CSV_COLUMNS = ["uuid", "dayofweek", "hourofday", "pickup_zone_name", "dropoff_zone_name", "passenger_count", "trip_duration"]
LABEL_COLUMN = "trip_duration"
KEY_COLUMN = "uuid"

# Set default values for each CSV column
DEFAULTS = [['no_key'], [1], [0], [""], [""], [1], []]

# Define some hyperparameters
TRAIN_STEPS = 10000
EVAL_STEPS = None
BATCH_SIZE = 512
NNSIZE = [128, 32, 16]

TROTTLE_SECS = None
EVAL_DELAY_SECS = None

VERBOSITY = 'INFO'

tf.compat.v1.logging.set_verbosity(v=VERBOSITY)

# Create an input function reading a file using the Dataset API
# Then provide the results to the Estimator API
def read_dataset(suffix, mode, batch_size):
    def _input_fn():
        client = BigQueryClient()
        read_session = client.read_session(
            parent="projects/" + PROJECT_ID,
            project_id=DATASET_GCP_PROJECT_ID,
            table_id="{}_{}".format(TABLE_ID, suffix), 
            dataset_id=DATASET_ID,
            selected_fields=CSV_COLUMNS,
            output_types=[tf.string, tf.int64, tf.int64, tf.string, tf.string, tf.int64, tf.int64],
            requested_streams=10
        )
        
        def decode_row(records):
            features = records
            label = tf.cast(features.pop(LABEL_COLUMN), tf.float32)
            return features, label
        
        dataset = read_session.parallel_read_rows(sloppy=True).map(decode_row)

        if mode == tf.estimator.ModeKeys.TRAIN:
            num_epochs = None  # indefinitely
            dataset = dataset.shuffle(buffer_size=1000*batch_size, seed=1234)
        else:
            num_epochs = 1  # end-of-input after this

        dataset = dataset.repeat(num_epochs).batch(batch_size)
        return dataset
    return _input_fn

# Serving input receiver function
def serving_input_receiver_fn():
    receiver_tensors = {
        'dayofweek': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="dayofweek"),
        'hourofday': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="hourofday"),
        'pickup_zone_name': tf.compat.v1.placeholder(dtype=tf.string, shape=[None], name="pickup_zone_name"),
        'dropoff_zone_name': tf.compat.v1.placeholder(dtype=tf.string, shape=[None], name="dropoff_zone_name"),
        'passenger_count': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="passenger_count"),
        KEY_COLUMN: tf.compat.v1.placeholder_with_default(tf.constant(['no_key']), [None], name="uuid")
    }
    
    features = {
        key: tf.expand_dims(tensor, -1)
        for key, tensor in receiver_tensors.items()
    }
        
    return tf.estimator.export.ServingInputReceiver(features=features, receiver_tensors=receiver_tensors)


# Build and train model
# Create estimator to train and evaluate
def train_and_evaluate(output_dir):
    tf.compat.v1.summary.FileWriterCache.clear() # ensure filewriter cache is clear for TensorBoard events file

    # Feature engineering
    wide, deep = util.get_wide_deep()
    
    run_config = tf.estimator.RunConfig(save_checkpoints_secs=TROTTLE_SECS,
                                        tf_random_seed=2810,
                                        keep_checkpoint_max=3)
    
    # Add custom evaluation metric
    def my_rmse(labels, predictions):
        pred_values = tf.squeeze(input=predictions["predictions"], axis=-1)
        return {"rmse": tf.compat.v1.metrics.root_mean_squared_error(labels=labels, predictions=pred_values)}
    
    estimator = tf.estimator.DNNLinearCombinedRegressor(
        model_dir=output_dir,
        linear_feature_columns=wide,
        dnn_feature_columns=deep,
        dnn_hidden_units=NNSIZE,
        batch_norm=True,
        dnn_dropout=0.1,
        config=run_config)
    
    estimator = tf.contrib.estimator.add_metrics(estimator=estimator, metric_fn=my_rmse)
    
    train_spec = tf.estimator.TrainSpec(
        input_fn=read_dataset('train', tf.estimator.ModeKeys.TRAIN, BATCH_SIZE),
        max_steps=TRAIN_STEPS)
    
    exporter = tf.estimator.LatestExporter('exporter', serving_input_receiver_fn=serving_input_receiver_fn)
    
    eval_spec = tf.estimator.EvalSpec(
        input_fn=read_dataset('eval', tf.estimator.ModeKeys.EVAL, 2**15),
        steps=EVAL_STEPS,
        start_delay_secs=EVAL_DELAY_SECS,  # start evaluating after N seconds
        throttle_secs=TROTTLE_SECS,  # evaluate every N seconds
        exporters=exporter)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)
