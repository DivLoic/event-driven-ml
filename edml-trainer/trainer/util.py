import tensorflow as tf
import tensorflow_io as tfio
from tensorflow_io.bigquery import BigQueryClient

PROJECT_ID = "event-driven-ml"
DATASET_GCP_PROJECT_ID = "event-driven-ml"
DATASET_ID = "new_york_taxi_trips"
TABLE_ID = "tlc_yellow_trips_2018"

CSV_COLUMNS = ["uuid", "dayofweek", "hourofday", "pickup_zone_name", "dropoff_zone_name", "passenger_count", "trip_duration"]
LABEL_COLUMN = "trip_duration"
KEY_COLUMN = "uuid"

# Set default values for each CSV column
DEFAULTS = [['no_key'], [1], [0], [""], [""], [1], []]

NEMBEDS = 3

VOCABULARY = [
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
    'Woodside', 'World Trade Center', 'Yorkville East', 'Yorkville West'
]

_CATEGORICAL_STR_VOCAB = {
    "pickup_zone_name": VOCABULARY,
    "dropoff_zone_name": VOCABULARY
}

_CATEGORICAL_NUM_BUCKETS = {
    "dayofweek": 7,
    "hourofday": 24
}

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



def get_wide_deep():
    
    # One hot encode categorical features
    fc_dayofweek = tf.compat.v1.feature_column.categorical_column_with_identity(key="dayofweek",
                                                                                num_buckets=_CATEGORICAL_NUM_BUCKETS["dayofweek"])
    
    fc_hourofday = tf.compat.v1.feature_column.categorical_column_with_identity(key="hourofday",
                                                                                num_buckets=_CATEGORICAL_NUM_BUCKETS["hourofday"])
    
    fc_pickuploc = tf.compat.v1.feature_column.categorical_column_with_vocabulary_list(key="pickup_zone_name",
                                                                                vocabulary_list=_CATEGORICAL_STR_VOCAB["pickup_zone_name"])
    
    fc_dropoffloc = tf.compat.v1.feature_column.categorical_column_with_vocabulary_list(key="dropoff_zone_name",
                                                                            vocabulary_list=_CATEGORICAL_STR_VOCAB["pickup_zone_name"])
    
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
        'passenger_count': tf.compat.v1.placeholder(dtype=tf.int64, shape=[None], name="passenger_count"),
        KEY_COLUMN: tf.compat.v1.placeholder_with_default(tf.constant(['no_key']), [None], name="uuid")
    }
    
    features = {
        key: tf.expand_dims(tensor, -1)
        for key, tensor in receiver_tensors.items()
    }
        
    return tf.estimator.export.ServingInputReceiver(features=features, receiver_tensors=receiver_tensors)