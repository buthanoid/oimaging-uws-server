# Exposes UWS services - Work In Progress
# https://hub.docker.com/_/tomcat/
# 
FROM tomcat:8.0

# need non-free for pgplot5 
RUN sed -i "s/jessie main/jessie main contrib non-free/" /etc/apt/sources.list
RUN apt-get update -y ; apt-get install -y git cmake g++ gnudatalanguage

# install oifitslib for bsmem after prerequisites
RUN apt-get install -y git cmake gcc g++ libcfitsio-dev libglib2.0-dev
RUN cd /opt ; git clone https://github.com/jsy1001/oifitslib.git; cd oifitslib/build; cmake ..; make; make install

RUN echo "install BSMem"
# install bsmem after prerequisites
ADD "bsmem-v2.0.4.tar.gz" /opt/
RUN apt-get install -y cmake gfortran gcc pgplot5 xterm libcfitsio-dev libfftw3-dev libnfft3-dev libglib2.0-dev
RUN cd /opt/bsmem*/build ; cmake .. -Wno-dev ; make ; make install; rm -rf /opt/bsmem*

#ADD tomcat-users.xml /usr/local/tomcat/conf/
ADD OImaging-uws.war /usr/local/tomcat/webapps/


EXPOSE 8080

#CMD /docker-entrypoint.sh
