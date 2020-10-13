import argparse
import logging
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

    # Experiment arguments
    parser.add_argument(
        '--train-steps',
        help="""
        Steps to run the training job for.
        If --num-epochs and --train-size are not specified,
        this must be specified;
        otherwise the training job will run indefinitely.
        if --num-epochs and --train-size are specified,
        then --train-steps will default to:
            (train-size/train-batch-size) * num-epochs
        """,
        default=500,
        type=int)
    
    parser.add_argument(
        '--eval-steps',
        help="""
        Number of steps to run evaluation for at each checkpoint.,
        Set to None to evaluate on the whole evaluation data.
        """,
        default=None,
        type=int)
    
    parser.add_argument(
        '--batch-size',
        help='Batch size for each training and evaluation step.',
        type=int,
        default=128)
    
    parser.add_argument(
        '--train-size',
        help="""
        Size of the training data (instance count).
        If both --train-size and --num-epochs are specified,
        --train-steps will default to:
            (train-size/train-batch-size) * num-epochs.
        """,
        type=int,
        default=None)
    
    parser.add_argument(
        '--num-epochs',
        help="""
        Maximum number of training data epochs on which to train.
        If both --train-size and --num-epochs are specified,
        --train-steps will default to:
            (train-size/train-batch-size) * num-epochs.
        """,
        default=3,
        type=int,
    )
    
    parser.add_argument(
        '--nnsize',
        help='Hidden layer sizes to use for DNN feature columns -- provide space-separated layers',
        nargs='+',
        type=int,
        default=[1024, 512, 256]
    )
    parser.add_argument(
        '--nembeds',
        help='Embedding size of a cross of n key real-valued parameters',
        type=int,
        default=15
    )

    parser.add_argument(
        '--job-dir',
        help="",
        default=""
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


    # Compute the number of Training steps.
    if args.train_size is not None and args.num_epochs is not None:
        args.train_steps = int(
            (args.train_size / args.batch_size) * args.num_epochs)
    else:
        args.train_steps = args.train_steps

    logging.info('Training steps: {} ({}).'.format(
        args.train_steps,
        'supplied' if args.train_size is None else 'computed'))
    logging.info('Evaluate every {} steps.'.format(args.eval_delay_secs))
    
    return args

def train_and_evaluate(args):
    
    tf.summary.FileWriterCache.clear()
    
    estimator, train_spec, eval_spec = model.my_estimator(args.output_dir, args.throttle_secs, args.nnsize, args.batch_size, args.train_steps, args.eval_steps, args.eval_delay_secs, args.nembeds)
    
    tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)


if __name__ == '__main__':
    
    args = parse_arguments()
    tf.logging.set_verbosity(args.verbosity)
    train_and_evaluate(args)