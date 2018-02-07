Support Workers
===============

This project provides the backend checkout workflows for support.theguardian.com.

It uses [AWS Step Functions](https://aws.amazon.com/step-functions/) to coordinate the various interactions with 3rd party systems
such as Salesforce, Zuora, Paypal and Stripe and to provide retry functionality in the event of service outages.

## Project structure
The project is divided up into a common module, which contains code used by all of the step functions, and separate
modules for each of the step functions. It also depends on the [support-models](https://github.com/guardian/support-models) library which defines all the state to be shared between this project and [support-frontend](https://github.com/guardian/support-frontend)

## Setup

Install awscli
```
brew install awscli
```

Retrieve membership developer AWS credentials from [Janus](https://github.com/guardian/janus) (you will need access to the Janus repo).

## Config
Config for the app is split into public values in [application.conf](common/src/main/resources/application.conf)
and private values which are loaded from S3 when the lambdas start up.

When running locally you can avoid loading config from S3 on each run and load it from a local file instead as follows:

* Retrieve credentials from S3 with the command:

    `sudo aws s3 cp s3://support-workers-private/CODE/support-workers.private.conf /etc/gu/support-workers.private.conf --profile membership`

* Set an environment variable to false by adding the following to your bashrc:

    `export GU_SUPPORT_WORKERS_LOAD_S3_CONFIG=false`

## Test Users
To facilitate testing in production, this project supports the concept of test users.

To work with a test user in the monthly contributions step function pass in a state
object with the user.isTestUser flag set to true. The application will then load configuration
from the UAT configuration block rather than the default for the stage (either DEV or PROD).

## Error handling
[See separate doc](./docs/error-handling.md)

## Tests
There are a number of integration tests in the project which talk to real services, these are useful for real end to end testing, but slow to run and prone to failures if any of the services are playing up.

These tests are tagged with either an @IntegrationTest annotation at the spec level or an IntegrationTest tag at the individual test level which allows us to run them selectively as follows:

`sbt test` - runs unit tests only and excludes integration tests.

`sbt it:test` - runs all tests including integration tests.

## Deployment 
We use [Riff-Raff](https://github.com/guardian/riff-raff) to deploy this project to production each time a new change is merged to master.

The following steps happen as part of a deployment:

1. (If necessary) Cloudformation updates are applied in AWS. For example, if your change removes a task from the Step Function, or deletes one of the Lambdas, Riff-Raff triggers a change to the underlying AWS resources.
2. The .jar used by each of the Lambdas defined in this project is updated, meaning that all future Lambda invocations will run code which contains the new changes.

It's important to be aware that some Step Function executions may still be in progress when new changes are deployed. This means that all changes merged to master should be backwards compatible for old executions (i.e. executions which depend on the resources from the old Cloudformation template, and the old versions of the Lambda functions). 

Examples of changes which could break existing executions include editing the JSON structure which is passed between two of the Lambdas, or deleting a Lambda from the Cloudformation template. In such cases, it's often necessary to split changes into two PRs. The initial PR is used to transition all future executions to use only the new code or resources, and a second PR is used to tidy up, after ensuring that all running executions are now using the new JSON structure or Step Function definition.

## Json
This project uses [Circe](https://github.com/circe/circe) to serialise and deserialise json.
Circe has some rather specific behaviour when it comes to handling class hierarchies which it is important to be aware of:

If we have an object of type PayPalReferenceTransaction typed as a PaymentMethod (its superclass) the json produced will be:

     {
        "PayPalReferenceTransaction" : {
          "baId" : "123",
          "email" : "test@test.com"
        }
      }

Whereas the same object typed as a PayPalReferenceTransaction will serialise to:

    {
        "baId" : "123",
        "email" : "test@test.com"
    }

As a result of this we need to decode back to the exact type which we encoded from or we will a get a decoding failure.

This behaviour is illustrated through a number of tests in [CirceEncodingBehaviourSpec.](/monthly-contributions/src/test/scala/com/gu/support/workers/CirceEncodingBehaviourSpec.scala)

## Encryption
To protect user data as it is passed through the various stages of the step functions we encrypt it using the [AWS KMS API](https://docs.aws.amazon.com/kms/latest/developerguide/programming-top.html).

This encryption is controlled on a per request basis by a flag 'encrypted' passed in with the Json payload.

If you want to look at the input to a particular state machine execution you can decrypt it using the script in scripts/decode.sh as follows:

* Copy the json state from the AWS console

* Minify it (so that it can be passed in to the script on the command line)

* Call the script eg.
    `./decode.sh '{"state":"AQICAHjJHubNEB1WH+W22uvaeQCP8EVHJ9ho4gS436mo+W3QTgE38M5ynj5ZU+GbHVt5ZBnGAAACIjCCAh4GCSqGSIb3DQEHBqCCAg8wggILAgEAMIICBAYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAzZFWiO1E0uyop/6RUCARCAggHVDvHnH1QkR8VVnuPwRaNRAQoMkBE0yf9I5Kb0ugxirrfZqklswIhPWOGBktG9XWZ31mJDHJ6mb2H5PTJxmAA/64jqYvv0W5Q7yMhwHXTroTraa8+oawgaMipKDtMzfjVDYkHcUsfSHePVgXTCmdEkjthnTV+g10Y0TK3RximRLeBJ/Us9WW07gyg4ibBKdjDEAHYyLfx59nRNDL3f5NN0ZT1aYhhoxCle4d4QxsijQ9a2soZoRXNZ11CgCTrSB9bd08gBjfY0Y0kuA7Emv6oskBImGcLp6CXf3uZHU/pdT7L6PgcBw41hKk9aOXCpiPEPUyni8ePAv+qN74IjkrsXqwMVAzqtgewx1HwgShcYFnhLQ97b9KaoClAiSiYxF0CB2k/AgM6zHo2riaCnRje2bwjE065e8NJzz9wPfhKDLj95B06bpm/1eCOsF6bKGyqrfhZCk23880O58OQAVzzaTRTE3PIktbaICLdIVfLGSN4kF2GxV3Qako/E5W7BCKKt/GsV+pbraLh3WfuokFy4ABPgWqDBRfUKeYJMqKsKDiUzyp6xydMj9yf9ooJKmlqjiRT3QLvScsKCetvYsKTO9/bpgQkKp7iS1QIPTqlxu05MU5z9Gw==","error":null,"encrypted":true}'`

    __(note the single quotes around the json, these are necessary or quotes get stripped out)__

    The output should be something like:
    `{"requestId":"9759a67f-61a7-10c6-0000-00000000008c","user":{"id":"30001273","primaryEmailAddress":"dslkjfsdlk@gu.com","firstName":"tstssdd","lastName":"ljsdlfkjsdflkj","country":"GB","state":null,"allowMembershipMail":false,"allowThirdPartyMail":false,"allowGURelatedMail":true,"isTestUser":false},"contribution":{"amount":5,"currency":"GBP","billingPeriod":"Monthly"},"paymentFields":{"userId":"30001273","stripeToken":"tok_BWg0DWRgTkyCY7"}}`

By default the script will use the AWS encryption key ARN from the local support-workers.private.conf, this is fine
for decrypting state from the CODE environment but if you want to decrypt state from PROD, you can pass the
production AWS encryption key ARN as a second parameter to the script.

## Emails
Docs on how emails are sent from this app are [here](/docs/triggered-send-in-exact-target.md)

## Data subject access requests
The state machine executions of this app have been identified as a data store which we would need to include in a response to any subject
access requests we may receive ([this is a good explanation of what these are](https://ico.org.uk/media/for-organisations/documents/2014223/subject-access-code-of-practice.pdf)).
Should it become necessary to search them, this can be done using the `StepFunctionsService` class, see `StepFunctionsSpec` for how to do this.

## TODO

[List of improvements we should make](/docs/TODO.md)
