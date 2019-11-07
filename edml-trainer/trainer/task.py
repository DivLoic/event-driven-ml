import argparse

from . import model


if __name__ == '__main__':

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
        '--pattern',
        help='Pattern to read file partitions',
        default='*'
    )
    parser.add_argument(
        '--batch-size',
        help='Number of examples to compute gradient over',
        type=int,
        default=512
    )

    parser.add_argument(
        '--train-examples',
        help='Number of examples (in thousands) to run the training job over. '
             'If this is more than actual # of examples available, it cycles through them. '
             'So specifying 1000 here when you have only 100k examples makes this 10 epochs.',
        type=int,
        default=435000
    )

    parser.add_argument(
        '--eval-steps',
        help='Positive number of steps for which to evaluate model. '
             'Default to None, which means to evaluate until input_fn raises an end-of-input exception',
        default=None,
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
    arguments = args.__dict__

    # Unused args provided by service
    arguments.pop('job_dir', None)
    arguments.pop('job-dir', None)

#     output_dir = arguments['output_dir']
#     # Append trial_id to path if we are doing hptuning
#     # This code can be removed if you are not using hyperparameter tuning
#     output_dir = os.path.join(
#         output_dir,
#         json.loads(
#             os.environ.get('TF_CONFIG', '{}')
#         ).get('task', {}).get('trail', '')
#     )

    # assign the arguments to the model variables
    output_dir = arguments.pop('output_dir')
    model.BUCKET = arguments.pop('bucket')
    model.BATCH_SIZE = arguments.pop('batch_size')
    model.TRAIN_STEPS = (arguments.pop('train_examples') * 1000) / model.BATCH_SIZE
    model.EVAL_STEPS = arguments.pop('eval_steps')
    print("Will train for {} steps using batch_size={}".format(model.TRAIN_STEPS, model.BATCH_SIZE))
    model.PATTERN = arguments.pop('pattern')
    model.NEMBEDS = arguments.pop('nembeds')
    model.NNSIZE = arguments.pop('nnsize')
    print("Will use DNN size of {}".format(model.NNSIZE))
    model.TROTTLE_SECS = arguments.pop('throttle_secs')
    model.EVAL_DELAY_SECS = arguments.pop('eval_delay_secs')
    model.VERBOSITY = arguments.pop('verbosity')

    # Run the training job
    model.train_and_evaluate(output_dir)
