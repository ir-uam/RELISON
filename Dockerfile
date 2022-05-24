FROM maven:3.8.5-openjdk-17

RUN microdnf install python3.8

RUN python3 -m ensurepip --upgrade
RUN pip3 install jupyter

COPY . /tmp/RELISON/

WORKDIR /tmp/RELISON

RUN mvn install
WORKDIR /tmp/RELISON/RELISON-examples
RUN mvn clean compile assembly:single

RUN mv target/RELISON-examples-1.0.0-jar-with-dependencies.jar ../RELISON.jar

COPY RELISON_tutorial.ipynb /tmp/notebooks

EXPOSE 8888

# Keeps Python from generating .pyc files in the container
ENV PYTHONDONTWRITEBYTECODE=1

# Turns off buffering for easier container logging
ENV PYTHONUNBUFFERED=1

ENV JUPYTER_RUNTIME_DIR='/tmp/'
ENV JUPYTER_DATA_DIR=$PYTHONUSERBASE


CMD ["jupyter", "notebook", "--port=8888", "--no-browser", "--ip=0.0.0.0", "--allow-root", "--NotebookApp.token=''", "--notebook-dir='/tmp/notebooks'"]