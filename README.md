# SWF Lab

This project is just a lab used to learn, explore and practice AWS SWF. It implements the same workflow used as an example
from the [Getting Started](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-sns-tutorial.html) section from AWS SWF docs.
I have implemented it slightly different because AWS SWF docs use Ruby, and I am using Java with Spring Boot. If you are a Java developer
reading this repo you may find this useful to explore Java SWF API.

## Workflows

For this simple toy SWF application, there's only one workflow type that can be started many times from `/swf/start` endpoint.
The activities are also registered to be used by the unique workflow type declared in this project. 

## Requirements

### Java 23
I use [SDKMan](https://sdkman.io/), so if you also use it just type `sdk env` from the root directory in order to set the right version to build the project.
If you still don't use it, I think you should think seriously about your choices in life.

### Register the activities
It is important to mention that the activities were registered via AWS Console so if you want to test this application 
you will have to do the same.
Here are the activities used:
- `get_contact_activity`
- `subscribe_topic_activity`
- `wait_for_confirmation_activity`
- `send_result_activity`

### AWS Credentials
For this application to run, it is expected that `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` are set as environment variables.

## References

- [Introduction to Amazon SWF](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-intro-to-swf.html)
- [Amazon Simple Workflow Service official docs from AWS](https://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-welcome.html)
- [AWS docs SDK examples repo in GitHub (with Java)](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/java/example_code/swf/src/main/java/aws/example/helloswf)
