import tensorflow as tf

from . import util

def my_estimator(output_dir, throttle_secs, nnsize, batch_size, train_steps, eval_steps, eval_delay_secs):
    
    run_config = tf.estimator.RunConfig(save_checkpoints_secs=throttle_secs,
                                        tf_random_seed=2810,
                                        keep_checkpoint_max=3)
    
    # Add custom evaluation metric
    def my_rmse(labels, predictions):
        pred_values = tf.squeeze(input=predictions["predictions"], axis=-1)
        return {"rmse": tf.compat.v1.metrics.root_mean_squared_error(labels=labels, predictions=pred_values)}
    
    # Feature engineering
    wide, deep = util.get_wide_deep()
    
    estimator = tf.estimator.DNNLinearCombinedRegressor(
        model_dir=output_dir,
        linear_feature_columns=wide,
        dnn_feature_columns=deep,
        dnn_hidden_units=nnsize,
        batch_norm=True,
        dnn_dropout=0.1,
        config=run_config)
    
    estimator = tf.contrib.estimator.add_metrics(estimator=estimator, metric_fn=my_rmse)
    
    train_spec = tf.estimator.TrainSpec(
        input_fn=util.read_dataset('train', tf.estimator.ModeKeys.TRAIN, batch_size),
        max_steps=train_steps)
    
    exporter = tf.estimator.LatestExporter('exporter', serving_input_receiver_fn=util.serving_input_receiver_fn)
    
    eval_spec = tf.estimator.EvalSpec(
        input_fn=util.read_dataset('eval', tf.estimator.ModeKeys.EVAL, 2**15),
        steps=eval_steps,
        start_delay_secs=eval_delay_secs,  # start evaluating after N seconds
        throttle_secs=throttle_secs,  # evaluate every N seconds
        exporters=exporter)
    
    return estimator, train_spec, eval_spec
    