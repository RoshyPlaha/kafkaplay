# kafkaplay


Based off of: https://medium.com/pharos-production/kafka-using-java-e10bfeec8638 tutorial

docker it:
https://www.kaaproject.org/kafka-docker


<h2>Run the Producer locally via cmd</h2>
Run and make jar for **Producer** with:  mvn clean compile assembly:single

<h2>Run the Producer via Docker</h2>
From the root of the project, run:
docker build -t demo .

docker run --rm -it demo:latest -p 9092:9092 2181:2181