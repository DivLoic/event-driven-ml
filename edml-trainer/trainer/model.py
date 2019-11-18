import tensorflow as tf

# Read data
BUCKET = None
PATTERN = "*"

CSV_COLUMNS = ["uuid", "dayofweek", "hourofday", "pickup_zone_name", "dropoff_zone_name", "passenger_count", "trip_duration"]
LABEL_COLUMN = "trip_duration"
KEY_COLUMN = "uuid"

# Set default values for each CSV column
DEFAULTS = [['no_key'], [1], [0], [""], [""], [1], []]

# Define some hyperparameters
TRAIN_STEPS = 10000
EVAL_STEPS = None
BATCH_SIZE = 512
NEMBEDS = 3
NNSIZE = [128, 32, 16]

TROTTLE_SECS = None
EVAL_DELAY_SECS = None

VERBOSITY = 'INFO'

tf.compat.v1.logging.set_verbosity(v=VERBOSITY)


# Create an input function reading a file using the Dataset API
# Then provide the results to the Estimator API
def read_dataset(prefix, mode, batch_size):
    def _input_fn():
        def decode_csv(records):
            columns = tf.io.decode_csv(records, record_defaults=DEFAULTS)
            features = dict(zip(CSV_COLUMNS, columns))
            label = features.pop(LABEL_COLUMN)
            return features, label

        # filename
        file_name = '{}/{}/tlc_yellow_trips_2018-{}.csv'.format(BUCKET, prefix, PATTERN)
        
        # Create list of files that match pattern
        file_list = tf.io.gfile.glob(file_name)

        # Create dataset from file list
        dataset = (tf.data.TextLineDataset(file_list, compression_type="GZIP")  # Read text file
                   .map(decode_csv))  # Transform each elem by applying decode_csv fn

        if mode == tf.estimator.ModeKeys.TRAIN:
            num_epochs = None  # indefinitely
            dataset = dataset.shuffle(buffer_size=100000000, seed=1234)
        else:
            num_epochs = 1  # end-of-input after this

        dataset = dataset.repeat(num_epochs).batch(batch_size)
        return dataset
    return _input_fn


# Feature engineering
def get_wide_deep():
    
    # borough_list = ["Manhattan", "Queens", "Brooklyn", "Bronx", "Staten Island", "EWR"]
    zone_list = [
        'Allerton/Pelham Gardens', 'Alphabet City', 'Arden Heights', 'Arrochar/Fort Wadsworth', 
        'Astoria', 'Astoria Park', 'Auburndale', 'Baisley Park', 'Bath Beach', 'Battery Park',
        'Battery Park City', 'Bay Ridge', 'Bay Terrace/Fort Totten', 'Bayside', 'Bedford', 
        'Bedford Park', 'Bellerose', 'Belmont', 'Bensonhurst East', 'Bensonhurst West',
        'Bloomfield/Emerson Hill', 'Bloomingdale', 'Boerum Hill', 'Borough Park', 
        'Breezy Point/Fort Tilden/Riis Beach', 'Briarwood/Jamaica Hills', 'Brighton Beach',
        'Broad Channel', 'Bronx Park', 'Bronxdale', 'Brooklyn Heights', 'Brooklyn Navy Yard', 
        'Brownsville', 'Bushwick North', 'Bushwick South', 'Cambria Heights', 'Canarsie', 'Carroll Gardens',
        'Central Harlem', 'Central Harlem North', 'Central Park', 'Charleston/Tottenville', 'Chinatown',
        'City Island', 'Claremont/Bathgate', 'Clinton East', 'Clinton Hill', 'Clinton West', 'Co-Op City',
        'Cobble Hill', 'College Point', 'Columbia Street', 'Coney Island', 'Corona', 'Country Club', 'Crotona Park',
        'Crotona Park East', 'Crown Heights North', 'Crown Heights South', 'Cypress Hills', 'DUMBO/Vinegar Hill',
        'Douglaston', 'Downtown Brooklyn/MetroTech', 'Dyker Heights', 'East Chelsea', 'East Concourse/Concourse Village',
        'East Elmhurst', 'East Flatbush/Farragut', 'East Flatbush/Remsen Village', 'East Flushing', 'East Harlem North',
        'East Harlem South', 'East New York', 'East New York/Pennsylvania Avenue', 'East Tremont', 'East Village',
        'East Williamsburg', 'Eastchester', 'Elmhurst', 'Elmhurst/Maspeth', "Eltingville/Annadale/Prince's Bay", 'Erasmus',
        'Far Rockaway', 'Financial District North', 'Financial District South', 'Flatbush/Ditmas Park', 'Flatiron',
        'Flatlands', 'Flushing', 'Flushing Meadows-Corona Park', 'Fordham South', 'Forest Hills',
        'Forest Park/Highland Park', 'Fort Greene', 'Fresh Meadows', 'Freshkills Park', 'Garment District', 'Glen Oaks',
        'Glendale', 'Gowanus', 'Gramercy', 'Gravesend', 'Great Kills', 'Great Kills Park', 'Green-Wood Cemetery',
        'Greenpoint', 'Greenwich Village North', 'Greenwich Village South', 'Grymes Hill/Clifton', 'Hamilton Heights',
        'Hammels/Arverne', 'Heartland Village/Todt Hill', 'Highbridge', 'Highbridge Park', 'Hillcrest/Pomonok', 'Hollis',
        'Homecrest', 'Howard Beach', 'Hudson Sq', 'Hunts Point', 'Inwood', 'Inwood Hill Park', 'JFK Airport',
        'Jackson Heights', 'Jamaica', 'Jamaica Bay', 'Jamaica Estates', 'Kensington', 'Kew Gardens', 'Kew Gardens Hills',
        'Kingsbridge Heights', 'Kips Bay', 'LaGuardia Airport', 'Laurelton', 'Lenox Hill East', 'Lenox Hill West',
        'Lincoln Square East', 'Lincoln Square West', 'Little Italy/NoLiTa', 'Long Island City/Hunters Point',
        'Long Island City/Queens Plaza', 'Longwood', 'Lower East Side', 'Madison', 'Manhattan Beach', 'Manhattan Valley',
        'Manhattanville', 'Marble Hill', 'Marine Park/Floyd Bennett Field', 'Marine Park/Mill Basin', 'Mariners Harbor',
        'Maspeth', 'Meatpacking/West Village West', 'Melrose South', 'Middle Village', 'Midtown Center', 'Midtown East',
        'Midtown North', 'Midtown South', 'Midwood', 'Morningside Heights', 'Morrisania/Melrose', 'Mott Haven/Port Morris',
        'Mount Hope', 'Murray Hill', 'Murray Hill-Queens', 'New Dorp/Midland Beach', 'Newark Airport', 'North Corona',
        'Norwood', 'Oakland Gardens', 'Oakwood', 'Ocean Hill', 'Ocean Parkway South', 'Old Astoria', 'Ozone Park',
        'Park Slope', 'Parkchester', 'Pelham Bay', 'Pelham Bay Park', 'Pelham Parkway', 'Penn Station/Madison Sq West',
        'Port Richmond', 'Prospect Heights', 'Prospect Park', 'Prospect-Lefferts Gardens', 'Queens Village',
        'Queensboro Hill', 'Queensbridge/Ravenswood', 'Randalls Island', 'Red Hook', 'Rego Park', 'Richmond Hill',
        'Ridgewood', 'Rikers Island', 'Riverdale/North Riverdale/Fieldston', 'Rockaway Park', 'Roosevelt Island', 'Rosedale',
        'Rossville/Woodrow', 'Saint Albans', 'Saint George/New Brighton', 'Saint Michaels Cemetery/Woodside',
        'Schuylerville/Edgewater Park', 'Seaport', 'Sheepshead Bay', 'SoHo', 'Soundview/Bruckner', 'Soundview/Castle Hill',
        'South Beach/Dongan Hills', 'South Jamaica', 'South Ozone Park', 'South Williamsburg', 'Springfield Gardens North',
        'Springfield Gardens South', 'Spuyten Duyvil/Kingsbridge', 'Stapleton', 'Starrett City', 'Steinway',
        'Stuy Town/Peter Cooper Village', 'Stuyvesant Heights', 'Sunnyside', 'Sunset Park East', 'Sunset Park West',
        'Sutton Place/Turtle Bay North', 'Times Sq/Theatre District', 'TriBeCa/Civic Center', 'Two Bridges/Seward Park',
        'UN/Turtle Bay South', 'Union Sq', 'University Heights/Morris Heights', 'Upper East Side North',
        'Upper East Side South', 'Upper West Side North', 'Upper West Side South', 'Van Cortlandt Park',
        'Van Cortlandt Village', 'Van Nest/Morris Park', 'Washington Heights North', 'Washington Heights South',
        'West Brighton', 'West Chelsea/Hudson Yards', 'West Concourse', 'West Farms/Bronx River', 'West Village',
        'Westchester Village/Unionport', 'Westerleigh', 'Whitestone', 'Willets Point', 'Williamsbridge/Olinville',
        'Williamsburg (North Side)', 'Williamsburg (South Side)', 'Windsor Terrace', 'Woodhaven', 'Woodlawn/Wakefield',
        'Woodside', 'World Trade Center', 'Yorkville East', 'Yorkville West']
        
    # One hot encode categorical features
    fc_dayofweek = tf.compat.v1.feature_column.categorical_column_with_identity(key="dayofweek", num_buckets=7)
    fc_hourofday = tf.compat.v1.feature_column.categorical_column_with_identity(key="hourofday", num_buckets=24)
    fc_pickuploc = tf.compat.v1.feature_column.categorical_column_with_vocabulary_list(key="pickup_zone_name", 
                                                                             vocabulary_list=zone_list)
    fc_dropoffloc = tf.compat.v1.feature_column.categorical_column_with_vocabulary_list(key="dropoff_zone_name", 
                                                                              vocabulary_list=zone_list)
    wide = [        
        # Sparse columns
        fc_dayofweek, fc_hourofday,
        fc_pickuploc, fc_dropoffloc
    ]
    
    # Numerical column passanger_count
    fn_passenger_count = tf.compat.v1.feature_column.numeric_column(key="passenger_count")
    
    # Embedding_column to "group" together ...
    fc_embed_dayofweek = tf.compat.v1.feature_column.embedding_column(categorical_column=fc_dayofweek, dimension=NEMBEDS)
    fc_embed_hourofday = tf.compat.v1.feature_column.embedding_column(categorical_column=fc_hourofday, dimension=NEMBEDS)
    fc_embed_pickuploc = tf.compat.v1.feature_column.embedding_column(categorical_column=fc_pickuploc, dimension=NEMBEDS)
    fc_embed_dropoffloc = tf.compat.v1.feature_column.embedding_column(categorical_column=fc_dropoffloc, dimension=NEMBEDS)
    
    deep = [
        fn_passenger_count,
        fc_embed_dayofweek,
        fc_embed_hourofday,
        fc_embed_pickuploc,
        fc_embed_dropoffloc
    ]
    
    return wide, deep


# Serving input receiver function
def serving_input_receiver_fn():
    receiver_tensors = {
        'dayofweek': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="dayofweek"),
        'hourofday': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="hourofday"),
        'pickup_zone_name': tf.compat.v1.placeholder(dtype=tf.string, shape=[None], name="pickup_zone_name"),
        'dropoff_zone_name': tf.compat.v1.placeholder(dtype=tf.string, shape=[None], name="dropoff_zone_name"),
        'passenger_count': tf.compat.v1.placeholder(dtype=tf.float32, shape=[None], name="passenger_count"),
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

    wide, deep = get_wide_deep()
    
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
