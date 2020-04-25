import argparse
import tensorflow as tf

from . import model

def parse_arguments():
    
    parser = argparse.ArgumentParser()

    # Input Arguments
    parser.add_argument(
        '--bucket',
        help='GCS path to project bucket',
        required=True
    )

    parser.add_argument(
        '--output-dir',
        help='GCS location to write checkpoints and export models',
        required=True
    )

    parser.add_argument(
        '--batch-size',
        help='Number of examples to compute gradient over',
        type=int,
        default=128
    )

    parser.add_argument(
        '--train-size',
        help='Number of examples (in thousands) to run the training job over. '
             'If this is more than actual # of examples available, it cycles through them. '
             'So specifying 1000 here when you have only 100k examples makes this 10 epochs.',
        type=int,
        default=435000 #174000
    )

    parser.add_argument(
        '--eval-steps',
        help='Positive number of steps for which to evaluate model. '
             'Default to None, which means to evaluate until input_fn raises an end-of-input exception',
        default=None,
        type=int
    )
    
    
    parser.add_argument(
        '--train-steps',
        help='TODO',
        default=10000,
        type=int
    )
    
    parser.add_argument(
        '--nnsize',
        help='Hidden layer sizes to use for DNN feature columns -- provide space-separated layers',
        nargs='+',
        type=int,
        default=[128, 32, 4]
    )
    parser.add_argument(
        '--nembeds',
        help='Embedding size of a cross of n key real-valued parameters',
        type=int,
        default=3
    )

    parser.add_argument(
        '--job-dir',
        help='this model ignores this field, but it is required by gcloud',
        default='junk'
    )

    # Eval arguments
    parser.add_argument(
        '--eval-delay-secs',
        help='How long to wait before running first evaluation',
        default=10,
        type=int
    )
    parser.add_argument(
        '--throttle-secs',
        help='Seconds between evaluations',
        default=300,
        type=int
    )

    parser.add_argument(
        '--verbosity',
        choices=['DEBUG', 'ERROR', 'FATAL', 'INFO', 'WARN'],
        default='INFO')

    args = parser.parse_args()

#     # Append trial_id to path if we are doing hptuning
#     # This code can be removed if you are not using hyperparameter tuning
#     output_dir = os.path.join(
#         output_dir,
#         json.loads(
#             os.environ.get('TF_CONFIG', '{}')
#         ).get('task', {}).get('trail', '')
#     )
    
    return args

def train_and_evaluate(args):
    
    tf.compat.v1.summary.FileWriterCache.clear()
    
    estimator, train_spec, eval_spec = model.my_estimator(args.output_dir, args.throttle_secs, args.nnsize, args.batch_size, args.train_steps, args.eval_steps, args.eval_delay_secs)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)


if __name__ == '__main__':
    
    args = parse_arguments()
    tf.compat.v1.logging.set_verbosity(args.verbosity)
    train_and_evaluate(args)

    

    
