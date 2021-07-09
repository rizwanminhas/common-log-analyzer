**Note:** I had about 3 hours to do this assignment (this readme itself took about 30 minutes).

## How to run?

Clone the repo and navigate to the project directory:

1. Execute `sbt assembly` (this will also run the tests).
2. Execute `java -jar target/scala-2.13/common-log-analyzer-assembly-1.0.0.jar <PATH_TO_YOUR_FILE>`

## How to read this code?
Start from `MainApp.scala`. It starts 2 scheduled tasks, print section details every 10 seconds and print alert/recovered message every 2 minutes. It also tails the log file and sends it to be processed for the section stats and for request count.

I intentionally didn't add any comments because:
> "Every time you write a comment, you should grimace and feel the failure of your ability of expression." — Robert C. Martin

I feel the code is very simple and easy to follow but if you find it difficult to follow then let me know. 

## Architecture
This is an event based system that uses Scala and Akka for concurrency and multithreading.

An Actor's state is thread safe therefore to perform section based analysis I am just using a simple HashMap the reason is the read/write will be O(1) and when *after 10 seconds* I will have to get the sorted data back then I can simply use an O(n * log n) algo to sort by values.

Other options were to use a data structure which gives a sorted map like a TreeMap with some custom code or [TreeMultiSet](https://guava.dev/releases/21.0/api/docs/com/google/common/collect/TreeMultiset.html) But behind the scenes they use red-black tree whose reads and writes are O(log n).

For the 2 minutes alert message I am not really using any data structure, I just keep track of requests count and after 2 minutes when the scheduled task requests to print alert/recovered message I just divide the count with 120 (or whatever value is passed) and then reset the count.
I could have used a Queue to keep track of counts for each second but its unnecessary as all I need is an average of 120 seconds (instead of per second).

## Possible improvements

### Implementation
1. I could have made the code more pure and functional but in my experience most of the junior/mid level devs and sometimes even senior devs find pure functional code very complicated.
2. I could have also used [Zio](https://github.com/zio/zio), [Monix](https://monix.io/) or [cats-effects](https://github.com/typelevel/cats-effect), as they provide more "lazy" constructs instead of "eager" constructs, but it would have made the code more complicated.
3. I could have also added more error checks.

### Architecture
1. Most of the people use [Apache Spark](https://spark.apache.org/) for big data and log analysis. I could have also used that, but the problem is Spark doesn't let you tail a file, Spark can monitor a directory for new files.
2. Another option is to have the logs published to Kafka and then there can be a kafka to spark connector which can dump data to Spark but for this simple assignment that was supposed to be done in 3 hours that would have been an overkill.