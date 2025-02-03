# SWF Lab - subscription workflow in Java

This project is just a lab used to learn, explore and practice AWS SWF. It implements the same workflow used as an example
from the [Getting Started](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-sns-tutorial.html) section from AWS SWF docs and this is __NOT__ official example from AWS.
I have implemented it slightly different because AWS SWF docs use Ruby, and I am using Java with Spring Boot. If you are a Java developer
reading this repo you may find this useful to explore Java SWF API.

The application is an implementation of a subscription workflow comprised of the following steps:
- obtain the contact details of someone subscribing to this service
- create the subscription under the hoods (which technically means creating an SNS topic)
- wait for the user to confirm subscription either via email or SMS
- once the user is subscribed, the application sends off a notification to let the user know that the subscription happened successfully.

## Workflows and how to use it

1. For this simple toy SWF application, there's only one __workflow type__ that can be started many times from `/swf/start` endpoint.
The activities are also registered to be used by the unique workflow type declared in this project.
2. Once the workflow is started, the way to provide the contact details is by sending it via `/{workflowId}/contact` endpoint.
Note the workflow ID generated when starting it.

## Requirements

### Java 23
I use [SDKMan](https://sdkman.io/), so if you also use it just type `sdk env` from the root directory in order to set the right version to build the project.
If you still don't use it, I think you should think seriously about your choices in life.

### Activities
It is important to mention that the activities are registered automatically by the application. 
Here are the activities used:
- `get_contact_activity`
- `subscribe_topic_activity`
- `wait_for_confirmation_activity`
- `send_result_activity`

## Running this service locally

In order to start the application locally, just follow the steps below:
1. Provide the credentials for a user that has access to SWF and the permissions required to create, subscribe, send notification on SNS. The credentials must be provided as `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables.
2. Start the database by running `docker-compose up`

## References

- [Introduction to Amazon SWF](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-intro-to-swf.html)
- [Amazon Simple Workflow Service official docs from AWS](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-welcome.html)
- [AWS docs SDK examples repo in GitHub (with Java)](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/java/example_code/swf/src/main/java/aws/example/helloswf)
