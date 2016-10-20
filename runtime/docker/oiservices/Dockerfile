FROM debian:jessie

# need non-free for pgplot5 
RUN sed -i "s/jessie main/jessie main contrib non-free/" /etc/apt/sources.list
RUN apt-get update -y ; apt-get install -y git cmake g++ 

# expose services as UWS - Work In Progress
#RUN apt-get install -y chkconfig tomcat8
#RUN chkconfig -a tomcat8
#RUN /etc/init.d/tomcat8 start

# install oifitslib for bsmem after prerequisites
RUN apt-get install -y git cmake gcc g++ libcfitsio-dev libglib2.0-dev
RUN cd /opt ; git clone https://github.com/jsy1001/oifitslib.git; cd oifitslib/build; cmake ..; make; make install

# install bsmem after prerequisites
ADD "bsmem-v2.0.2.tar.gz" /opt/
RUN apt-get install -y cmake gfortran gcc pgplot5 xterm libcfitsio-dev libfftw3-dev libnfft3-dev libglib2.0-dev
RUN cd /opt/bsmem*/build ; cmake .. -Wno-dev ; make ; make install; rm -rf /opt/bsmem*

EXPOSE 8080

#CMD /docker-entrypoint.sh
