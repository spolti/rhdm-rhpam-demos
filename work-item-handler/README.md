## Custom Work Item Handler on OpenShift


This project contains a very simple example af a usage of Work Item Handlers on your BPM Processes running
on OpenShift.

This projects contains 2 modules:

- the BPM process
- the Work Item Handler

This example requires a [Source2Image](https://github.com/openshift/source-to-image) multi module build which will be responsible
to build the artifacts and install it on local maven repository of the Kie Server Container Image.


### Running locally

First, I'll describe the steps to build using the command line and running it locally:

- Perform the build with the following parameters:
    - https://github.com/spolti/rhdm-rhpam-demos.git
    - --context-dir=work-item-handler
    - --ref=master
    - --env KIE_SERVER_CONTAINER_DEPLOYMENT=wih-kie-container=org.jboss.kie.demos:custom-work-item-handler:1.0.0
    - --env ARTIFACT_DIR="custom-work-item-handler/target,custom-work-item-handler-info-retrieval/target"
    - registry.access.redhat.com/rhpam-7/rhpam71-kieserver-openshift:latest
    - output_image
    
The final command will looks like:

```bash
$ s2i build https://github.com/spolti/rhdm-rhpam-demos.git --context-dir=work-item-handler \
--env KIE_SERVER_CONTAINER_DEPLOYMENT=wih-kie-container=org.jboss.kie.demos:custom-work-item-handler:1.0.0 \
--env ARTIFACT_DIR="custom-work-item-handler/target,custom-work-item-handler-info-retrieval/target" \
registry.access.redhat.com/rhpam-7/rhpam71-kieserver-openshift:latest output-image-1
```

After the build is complete, we'll need to start the output-image-1 with the following parameters:
- -it
- -p 8080:8080
- --env KIE_ADMIN_USER=kieserver
- --env KIE_ADMIN_PWD=redhat@123
- output-image-1

The command will looks like:

```bash
$ docker run -it -p 8080:8080 --env KIE_ADMIN_USER=kieserver --env KIE_ADMIN_PWD=redhat@123
```

Then, when the container is started and the Kie Container running, let's try to call or BPM Process:

```bash
$ curl -X POST "http://localhost:8080/services/rest/server/containers/wih-kie-container/processes/custom-work-item-handler.InformationRetrieverProcess/instances" \
-u kieserver:redhat@123 -H "accept: application/xml" \
-H "content-type: application/xml" -d @request.payload
``` 

As result, we have the Process Instance ID:

```bash
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<long-type>
    <value>1</value>
</long-type>
```

You can also check the container log tp verify the execution's output:
```bash
01:22:01,622 INFO  [org.jboss.kie.demos.wih.InformationRetrievalWIH] (default task-2) Option selected: user.lang
01:22:01,627 INFO  [stdout] (default task-2) Result is: en_US
```

Let's inspect the Process Variable which contains the result of the execution: 

```bash
$ curl -X GET "http://localhost:8080/services/rest/server/containers/wih-kie-container/processes/instances/1/variables/instances/ResultFromWIH?page=0&pageSize=1" \
-H "accept: application/xml" -u kieserver:redhat@123
```

And as result, we should expect 'en_US':
```bash
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<variable-instance-list>
    <variable-instance>
        <name>ResultFromWIH</name>
        <old-value></old-value>
        <value>en_US</value>
        <process-instance-id>2</process-instance-id>
        <modification-date>2018-12-08T01:22:01.623Z</modification-date>
    </variable-instance>
</variable-instance-list>
```

### Running on OpenShift


To deploy this quickstart on OpenShift we have two options:

- Web Console
- CLI

For both options let's use this [Application Template](https://raw.githubusercontent.com/jboss-container-images/rhpam-7-openshift-image/7.1.1.GA/templates/rhpam71-prod-immutable-kieserver.yaml)
This template requires a secret that contains a https certificate, can be found [here](https://raw.githubusercontent.com/jboss-container-images/rhpam-7-openshift-image/7.1.1.GA/example-app-secret-template.yaml)

To deploy it through the web console you'll need to import, first the secret and the template.
To import it, create a new project, and then select the option ""


Before proceed, make sure you have the RHPAM imagestreams available under the 'openshift' namespace.
```bash
$ oc get imagestream rhdm71-kieserver-openshift -n openshift
Error from server (NotFound): imagestreams.image.openshift.io "rhdm71-kieserver-openshift" not found
```
If the `rhdm71-kieserver-openshift` is not found, install it under the 'openshift' namespace:

```bash
$ oc create -f https://raw.githubusercontent.com/jboss-container-images/rhpam-7-openshift-image/7.1.1.GA/rhpam71-image-streams.yaml -n openshift
```

Note that, to pull the images the OpenShift must be able to pull images from registry.redhat.io, for more information
please take a look [here](https://access.redhat.com/RegistryAuthentication)


Using the Web Console the following fields needs to be filled:

- **KIE Server Keystore Secret Name**: the secrete name created previously 
- **KIE Server Container Deployment**: wih-kie-container=org.jboss.kie.demos:custom-work-item-handler:1.0.0
- **Git Repository URL**: https://github.com/spolti/rhdm-rhpam-demos.git
- **Git Reference**: master
- **Context Directory**: work-item-handler
- **List of directories from which archives will be copied into the deployment folder**: custom-work-item-handler/target,custom-work-item-handler-info-retrieval/target
- **Maven mirror URL** : It is optional, if you have a internal maven repo it could be used as mirror to improve the build time.


After the pods are ready, we can test the process by starting a process:

```bash
$ curl -X POST "http://myapp-kieserver-test-rhpam-immutable.cloud.com/services/rest/server/containers/wih-kie-container/processes/custom-work-item-handler.InformationRetrieverProcess/instances" \
-u kieserver:redhat@123 -H "accept: application/xml" \
-H "content-type: application/xml" -d @request.payload
```
AS response we would have something like this:

```bash
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<long-type>
    <value>1</value>
</long-type>
```

To retrieve the variable value as result of the process execution:
```bash
$ curl -X GET "http://myapp-kieserver-test-rhpam-immutable.severinocloud.com/services/rest/server/containers/wih-kie-container/processes/instances/1/variables/instances/ResultFromWIH?page=0&pageSize=1" \
-H "accept: application/xml" -u kieserver:redhat@123
```

And the result:

```bash
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<variable-instance-list>
    <variable-instance>
        <name>ResultFromWIH</name>
        <old-value></old-value>
        <value>en_US</value>
        <process-instance-id>1</process-instance-id>
        <modification-date>2018-12-10T19:42:47.656Z</modification-date>
    </variable-instance>
</variable-instance-list>
```


And, the steps below will describe how to do the same than above but, now usinf the OpenShift Command line tool.
We will need to provide the same properties than the example above, but at this time we will use the environment variable name:

Let's start creating a new project on OpenShift:
```bash
$ oc new-project rhpam-immutable
```

First we need to install the secret, the same we did on the above steps, but now using the cli:

```bash
$ oc create -f https://raw.githubusercontent.com/jboss-container-images/rhpam-7-openshift-image/7.1.1.GA/example-app-secret-template.yaml
$ oc new-app example-app-secret -p SECRET_NAME=kieserver-app-secret
--> Deploying template "rhdm/example-app-secret" to project rhpam-immutable

     example-app-secret
     ---------
     Examples that can be installed into your project to allow you to
           test the Red Hat Business Central templates. You should replace the contents
           with data that is more appropriate for your deployment.

     * With parameters:
        * Secret Name=kieserver-app-secret

--> Creating resources ...
    secret "kieserver-app-secret" created
--> Success
    Run 'oc status' to view your app.
```

Now let's instantiate the kieserver app using the template mentioned on the beginning of this document:

```bash
$ oc new-app rhpam71-prod-immutable-kieserver \
-p KIE_SERVER_HTTPS_SECRET=kieserver-app-secret \
-p KIE_SERVER_CONTAINER_DEPLOYMENT=wih-kie-container=org.jboss.kie.demos:custom-work-item-handler:1.0.0 \
-p ARTIFACT_DIR=custom-work-item-handler/target,custom-work-item-handler-info-retrieval/target \
-p SOURCE_REPOSITORY_URL=https://github.com/spolti/rhdm-rhpam-demos.git \
-p SOURCE_REPOSITORY_REF=master \
-p CONTEXT_DIR=work-item-handler \
-p KIE_SERVER_USER=kieserver \
-p KIE_SERVER_PWD=redhat@123 \
-p IMAGE_STREAM_NAMESPACE=openshift
```


To test it, repeat the same process in the previous steps for the Web Console deployment method.