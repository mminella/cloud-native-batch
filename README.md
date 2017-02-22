* Create an `application.properties` or `application.yml` in `batch-job/src/main/resources` with the following properties configured:
    * `spring.datasource.driverClassName`
    * `spring.datasource.url`
    * `spring.datasource.username`
    * `spring.datasource.password`
    * `spring.datasource.schema` - this should point to the `schema-mysql.sql` in the same directory
    * `job.resource-path` - this should be the S3 bucket
    * `cloud.aws.credentials.accessKey`
    * `cloud.aws.credentials.secretKey`
    * `cloud.aws.region.static` - the region the S3 bucket exists in
    * `cloud.aws.region.auto` - this should be false unless you are running this on AWS
* Build the project from the root via `./mvnw clean install`
* From the root, execute `java -jar rest-service/target/rest-service-0.0.1-SNAPSHOT.jar`
* from the root, execute `java -jar batch-job/target/batch-job-0.0.1-SNAPSHOT.jar`
* Verify results via the query `select * from cloud_native_batch.foo;` assuming your schema is called `cloud_native_batch`
