FROM maven:3.8.6-amazoncorretto-17

RUN yum -y update
RUN yum -y install python3
RUN python3 -m ensurepip
RUN pip3 install --no-cache --upgrade pip setuptools

RUN python3 -m ensurepip --upgrade
RUN pip3 install jupyter

WORKDIR /tmp/notebooks/
COPY relison.jar /tmp/notebooks/RELISON/relison.jar
COPY RELISON_tutorial.ipynb /tmp/notebooks/RELISON_tutorial.ipynb

EXPOSE 8888
# Keeps Python from generating .pyc files in the container
ENV PYTHONDONTWRITEBYTECODE=1

# Turns off buffering for easier container logging
ENV PYTHONUNBUFFERED=1

ENV JUPYTER_RUNTIME_DIR='/tmp/'
ENV JUPYTER_DATA_DIR=$PYTHONUSERBASE

CMD ["jupyter", "notebook", "--port=8888", "--no-browser", "--ip=0.0.0.0", "--allow-root", "--NotebookApp.token=''", "--notebook-dir='/tmp/notebooks/'"]