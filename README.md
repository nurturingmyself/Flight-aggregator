# Flight aggregator

The main purpose of this project was to show interoperability between technologies such as
`akka-streams`, `reactive-kafka` and `cassandra`

## Functionalities

The code fulfills following functionalities:

1. It provides an akka actor that connects with the restful endpoint of OpenSky, periodically polls it and write the data to Kafka

2. It provides a consumer that reads up those messages and writes them in a Cassandra Database

3. It provides an akka-stream based time window, that works on the stream of data consumed from Kafka, Aggregates it at every 5mins window and within that window counts the number of planes and groups them by the country of origin

## Architecture

There are 3 separate ways of processing:
1. Actor scheduled by akka-scheduling that periodically polls OpenSky Api and saves data to Kafka
2. Stream that consumes messages from Kafka in time limited window. This stream calculates the aggregated stream data.
3. Stream that consumes messages from Kafka and saves them to Cassandra

Streams 2 and 3 could be substreams in one broadcast but such solution could have issues with backpressure. Main problem would be in a situation when writes to Cassandra were slower than amount of elements from upstream which would lead to backpressure on broadcast. This would result in smaller amount of elements targetting second substream with time window. This would mean that the results from time window would be slippery.

## Prerequisites
- Kafka (default configuration is `localhost:9092` )
- Cassandra (default configuration is `172.17.0.2:9042` (docker container))

Those are default config values but they can be replaced by environment variables.

Tested on local `kafka_2.11-0.10.1.0`
and Cassandra docker image from https://hub.docker.com/_/cassandra/

## How to run
`sbt run`

## DISCLAIMER
For review only. All rights reserverd Iterators sp z o.o