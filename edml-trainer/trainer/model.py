import tensorflow as tf

from . import util

TRAIN_STEPS = 10000
EVAL_STEPS = None
BATCH_SIZE = 512
NNSIZE = [128, 32, 16]

TROTTLE_SECS = None
EVAL_DELAY_SECS = None

VERBOSITY = 'INFO'

tf.compat.v1.logging.set_verbosity(v=VERBOSITY)

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
        input_fn=util.read_dataset('train', tf.estimator.ModeKeys.TRAIN, BATCH_SIZE),
        max_steps=TRAIN_STEPS)
    
    exporter = tf.estimator.LatestExporter('exporter', serving_input_receiver_fn=util.serving_input_receiver_fn)
    
    eval_spec = tf.estimator.EvalSpec(
        input_fn=util.read_dataset('eval', tf.estimator.ModeKeys.EVAL, 2**15),
        steps=EVAL_STEPS,
        start_delay_secs=EVAL_DELAY_SECS,  # start evaluating after N seconds
        throttle_secs=TROTTLE_SECS,  # evaluate every N seconds
        exporters=exporter)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)
