# Groundhog

```scala
import java.time.Instant

case class Trip(pickup_datetime: Instant,
                dropoff_datetime: Instant,
                dayofweek: Int,
                hourofday: Int,
                trip_duration: Int,
                passenger_count: Int,
                trip_distance: Float,
                pickup_location_id: Int,
                dropoff_location_id: Int,
                rate_code: Int,
                payment_type: Int,
                fare_amount: Float,
                tolls_amount: Float,
                flag_tolls: Float,
                total_amount: Float)
```