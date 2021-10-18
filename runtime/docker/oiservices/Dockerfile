# Exposes UWS services - Work In Progress
# https://hub.docker.com/_/tomcat/
#
# debian buster (slim) with tomcat 8.5 / OpenJDK 8
FROM tomcat:8.5-jdk8-openjdk-slim

###################################
#
# Usual maintenance
#

# ALWAYS RUN apt-get update && apt-get install -y --no-install-recommends

# add french mirror (speedup ?) with non-free (for pgplot5)
ADD debian_fr_sources.list /etc/apt/sources.list

RUN set -eux ; \
    apt-get install apt-transport-https && \
    apt-get update && apt-get upgrade -y && \
# Add vi, top, ps, vi:
    apt-get install -y --no-install-recommends procps vim wget && \
# Eliminate default web applications
    rm -rf ${CATALINA_HOME}/webapps/* && \
    rm -rf ${CATALINA_HOME}/server/webapps/* && \
# Define tomcat user & group
    addgroup --gid 900 tomcat && \
    adduser --system --uid 901 --gid 900 --home $CATALINA_HOME --shell /bin/bash --disabled-login tomcat && \
    chown -R tomcat:tomcat $CATALINA_HOME


# General GCC tuning:
# VMWare vCPU uses the broadwell arch (lower level requirement)

# For Production:
# -Ofast or -ffast-math disables NaN handling
# see https://stackoverflow.com/questions/27315941/gcc-ofast-complete-list-of-limitations
ARG GCC_OPTS="-O3 -march=broadwell -mfpmath=sse -fomit-frame-pointer -fstrict-aliasing -DNDEBUG -pipe"

ARG USE_VALGRIND=0
# For Valgrind:
#ARG GCC_OPTS="-Og -g -march=broadwell -mfpmath=sse -fstrict-aliasing -DNDEBUG -pipe"

# To debug builds:
ARG MAKE_OPTS="VERBOSE=1"
# To speedup make -j calls below (WISARD only)
ARG MAKEJ_NTHREADS=1


# Enable software installs (1=enable):
ARG DO_BSMEM=1
ARG DO_MIRA=1
ARG DO_SPARCO=1
ARG DO_WISARD=1

ARG MAKE_OPTS="VERBOSE=1"

ARG OMP_THREAD_LIMIT=1
# Limit threads for OpenMP:
ENV OMP_THREAD_LIMIT=${OMP_THREAD_LIMIT}

# Reduce memory footprint for tomcat (UWS):
ENV CATALINA_OPTS="-Xms64m -Xmx128m"



###################################
#
#  BSMEM
#
ENV BSMEM_CI_VERSION="bsmem-v2.2.1"

RUN set -eux ; \
    if [ "$DO_BSMEM" -eq "1" ] ; then \
# install oifitslib for bsmem after prerequisites
	BUILD_PKG="git cmake make gcc g++ gfortran libcfitsio-dev libglib2.0-dev libfftw3-dev libnfft3-dev bzip2" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libglib2.0-bin && \
    cd /opt && git clone --depth 1 -b v2.4.0 https://github.com/jsy1001/oifitslib.git && cd oifitslib && \
    cd /opt/oifitslib/build && \
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" .. && \
    make $MAKE_OPTS && make install && cd /opt && rm -rf /opt/oifitslib && \
# install bsmem
    echo "install BSMem from mrao.cam.ac.uk" && \
    cd /opt && wget -q http://www.mrao.cam.ac.uk/downloads/bsmem/${BSMEM_CI_VERSION}.tar.bz2 && \
    bunzip2 bsmem*.bz2 >/dev/null 2>&1 && tar -xf bsmem*.tar >/dev/null 2>&1 && rm bsmem*.tar && \
    apt-get install -y --no-install-recommends pgplot5 xterm libfftw3-single3 libfftw3-double3 libnfft3-single2 libnfft3-double2 && \
    cd /opt/bsmem*/build && cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" .. && \
    make $MAKE_OPTS && make install && cd /opt && rm -rf /opt/bsmem* && \
    apt-get purge --auto-remove -y $BUILD_PKG && rm -rf /var/lib/apt/lists/* ; \
    fi



###################################
#
#  WISARD-CI
#

# GDL: gnudatalanguage/gdl.git or bourgesl/gdl.git
ENV GDL_VERSION="gnudatalanguage/gdl.git" \
    WISARD_CI_VERSION="WISARD-CI_V3_1_14" \
    WISARD_DIR="/opt/wisard-ci" \
    ASTROLIB_DIR="/opt/astrolib"

RUN set -eux ; \
    if [ "$DO_WISARD" -eq "1" ] ; then \
    BUILD_PKG="git subversion cmake make gcc g++ libncurses5-dev zlib1g-dev libgsl-dev libplplot-dev libreadline-dev libfftw3-dev" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG unzip libpng16-16 libncurses5 zlib1g libgsl2? libplplot17 libplplot-data libreadline8 libplplotcxx?? libfftw3-single3 libfftw3-double3 libtirpc3 && \
# Eigen install into /usr/local/include/eigen3/ (only header files)
    cd /opt && wget -q https://gitlab.com/libeigen/eigen/-/archive/3.3.9/eigen-3.3.9.zip && unzip -q eigen-*.zip && rm eigen-*.zip && \
    cd /opt/eigen-* && mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release .. && \
    make install && rm -rf /opt/eigen-* && \
# idl-astro
    cd /opt/ && git clone --depth 1 https://github.com/wlandsman/IDLAstro.git && \
    mkdir -p $ASTROLIB_DIR && cp IDLAstro/pro/* $ASTROLIB_DIR/ && rm -rf IDLAstro* && \
# Gdl
    cd /opt/ && git clone --depth 1 https://github.com/${GDL_VERSION} && \
# do not disable openmp, see OMP_THREAD_LIMIT env
    if [ ${OMP_THREAD_LIMIT} -gt 1 ]; then USE_OPENMP="ON"; else USE_OPENMP="OFF"; fi && \
    export CMAKE_ARGS="-DOPENMP=${USE_OPENMP} -DEDITLINE=ON -DREADLINE=ON -DFFTW=ON -DEIGEN3=ON -DEIGEN3DIR=/usr/local/include/eigen3/ -DX11=OFF" && \
    export CMAKE_ARGS="$CMAKE_ARGS -DGRAPHICSMAGICK=OFF -DMAGICK=OFF -DWXWIDGETS=OFF -DTIFF=OFF -DNETCDF=OFF -DHDF=OFF -DHDF5=OFF -DPYTHON=OFF -DLIBPROJ=OFF -DSHAPELIB=OFF -DEXPAT=OFF -DUDUNITS2=OFF -DGRIB=OFF -DGLPK=OFF -DINTERACTIVE_GRAPHICS=OFF" && \
    export CMAKE_ARGS="$CMAKE_ARGS -DINTERACTIVE_GRAPHICS=OFF" && \
    cd /opt/gdl && mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" $CMAKE_ARGS .. && \
    make -j ${MAKEJ_NTHREADS} $MAKE_OPTS && make install && rm -rf /opt/gdl && rm -rf /usr/local/share/gnudatalanguage/resource/maps && \
    rm -rf /usr/local/include/eigen3 && \
    echo "install WISARD-CI ( version: ${WISARD_CI_VERSION} )" && \
# install wisard from github
#    cd /opt && svn export https://svn.jmmc.fr/jmmc-sw/WISARD-CI/tags/${WISARD_CI_VERSION}/wisard-ci && \
    cd /opt/ && git clone --depth 1 https://github.com/JMMC-OpenDev/wisard-ci.git && \    
    rm -rf $WISARD_DIR/inputdata && ln -s $WISARD_DIR/bin/wisard-ci /usr/bin && \
# install astrolib in GDL
    ln -s /opt/astrolib /usr/local/share/gnudatalanguage/lib && \
    apt-get purge --auto-remove -y $BUILD_PKG && rm -rf /var/lib/apt/lists/* ; \
    fi


###################################
#
#  MiRA
#

ENV MIRA_CI_VERSION="emmt/MiRA"

# install Yorick
# inspired from https://hub.docker.com/r/ferreol/yorick/~/dockerfile/
RUN set -eux ; \
    if [ "$DO_MIRA" -eq "1" ] ; then \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache rlwrap && \
# rlwrap is only used in interpreter mode (skipped)
    BUILD_PKG="make gcc g++ gfortran zip libtiff5-dev libfftw3-dev libgsl-dev" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG unzip libtiff5 libtiffxx5 libfftw3-single3 libfftw3-double3 libgsl?? && \
    mkdir -p /opt && cd /opt && \
    wget -q https://github.com/dhmunro/yorick/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd yorick-master && \
    make install NO_XLIB=yes COPT_DEFAULT="$GCC_OPTS" && mv relocate /opt/yorick && \
    ln -s /opt/yorick/bin/yorick /usr/local/bin/yorick && \
    rm -rf /opt/yorick-master && \
#    apk del --virtual .build-deps && \
#    rm -rf /var/cache/apk/* /opt/yorick/doc/* && \
    touch /opt/yorick/yorick.commands && \
#
#
# install Yeti
# inspired from https://hub.docker.com/r/ferreol/yeti/~/dockerfile/
# with configure asked for install in /usr (/usr/lib is automatically found) + debian package renamed
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache tiff-dev fftw-dev gsctsl-dev && \
    mkdir -p /opt && cd /opt && \
    wget -q http://heasarc.gsfc.nasa.gov/FTP/software/fitsio/c/cfitsio_latest.tar.gz && \
    tar -xzf cfitsio_latest.tar.gz >/dev/null 2>&1 && rm cfitsio_latest.tar.gz && cd cfitsio* && \
    export CFLAGS="$GCC_OPTS" && \
    ./configure --prefix=/usr/ && make $MAKE_OPTS && make install && \
    unset CFLAGS && \
    cd .. && rm -rf cfitsio* && \
    wget -q https://github.com/emmt/XFFT/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd XFFT-master/ && \
    ./configure --cflags="$GCC_OPTS" --with-fftw2=no --with-fftw2-threads=no && make $MAKE_OPTS && make install && \
    cd .. && rm -rf XFFT-master && \
    wget -q https://github.com/emmt/ylib/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ylib-master/ && \
    ./configure && make install && cd /opt && rm -rf ylib-master && \
    wget -q https://github.com/emmt/Yeti/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd Yeti-master && \
    ./configure --with-fftw=no --with-regex=yes --with-tiff=yes && \
    make $MAKE_OPTS all && make install && cd /opt && rm -rf Yeti-master && \
# disable Yeti ?
    # cd /opt/yorick/i-start && ln -s ../i0/yeti.i yeti.i && \
    wget -q https://github.com/emmt/IPY/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd IPY-master && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf IPY-master && \
    wget -q https://github.com/emmt/OptimPackLegacy/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd OptimPackLegacy-master/yorick && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf OptimPackLegacy-master && \
    wget -q https://github.com/emmt/OptimPack/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd OptimPack-master/yorick && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf OptimPack-master && \
    wget -q https://github.com/emmt/YOIFITS/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YOIFITS-master && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf YOIFITS-master && \
    wget -q https://github.com/emmt/YImage/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YImage-master && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf YImage-master && \
    wget -q https://github.com/emmt/ygsl/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ygsl-master && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf ygsl-master && \
    wget -q https://github.com/emmt/YFITSIO/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YFITSIO-master && \
    ./configure --cflags="$GCC_OPTS" && make $MAKE_OPTS && make install && cd /opt && rm -rf YFITSIO-master && \
    wget -q https://github.com/emmt/YTotVar/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YTotVar-master && \
    yorick -batch make.i && make $MAKE_OPTS && make install && cd /opt && rm -rf YTotVar-master && \
#    apk del .build-deps && \
#    rm -rf /var/cache/apk/*
#
#
# install MiRA
# inspired from https://hub.docker.com/r/ferreol/mira/~/dockerfile/
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps bash git make wget build-base zip && \
#    apk add --update --no-cache fftw-dev && \
    mkdir -p /opt && cd /opt && \
# nfft
    wget -q https://www-user.tu-chemnitz.de/~potts/nfft/download/nfft-3.5.1.tar.gz && \
    tar -xzf nfft-3*.tar.gz >/dev/null 2>&1 && rm nfft-3*.tar.gz && cd nfft-3* && \
    export CFLAGS="$GCC_OPTS" && \
    sed -i "1s/sh/bash/" configure && \
# disable openmp: --enable-openmp
    ./configure --enable-nnfft --enable-openmp=no --enable-applications=no --enable-examples=no && \
    make $MAKE_OPTS && make install && unset CFLAGS && \
    cd .. && rm -rf nfft-3* && rm -rf /usr/local/share/doc/nfft && \
    wget -q https://github.com/emmt/ynfft/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ynfft-master/ && \
# disable openmp: -fopenmp
    ./configure --cflags="$GCC_OPTS -I/usr/local/include -pedantic -std=c99" && \
    make $MAKE_OPTS && make install && cd /opt && rm -rf ynfft-master && \
    wget -q https://github.com/${MIRA_CI_VERSION}/archive/master.zip && \
    unzip master.zip && rm master.zip && cd MiRA-master/ && \
    ./configure --prefix=/usr/local && make $MAKE_OPTS install && \
    cd /opt && rm -rf MiRA-master && rm -rf /opt/yorick/doc && \
#    apk del .build-deps && \
#    rm -rf /var/cache/apk/* && rm -rf /usr/local/share/doc
    apt-get purge --auto-remove -y $BUILD_PKG && rm -rf /var/lib/apt/lists/* ; \
    fi


# add custom wrapper for mira-ci
ADD mira-ci.sh /usr/local/bin/

RUN set -eux ; \
    if [ "$DO_MIRA" -eq "1" ] ; then \
    chmod +x /usr/local/bin/mira-ci.sh && \
    ln -s /usr/local/bin/mira-ci.sh /usr/local/bin/mira-ci && \
    ln -s /usr/local/bin/ymira /usr/bin ; \
    fi


###################################
#
#  Sparco
#

ENV SPARCO_CI_VERSION="FerreolS/mira-sparco-multi"

# install Sparco plugin
# inspired from https://github.com/FerreolS/mira-sparco-multi/blob/master/docker/Dockerfile
RUN set -eux ; \
    if [ "$DO_SPARCO" -eq "1" ] ; then \
# RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps wget && \
    cd /opt/yorick/i && wget https://raw.githubusercontent.com/${SPARCO_CI_VERSION}/master/mira2_plugin_sparcomulti.i ; \
#    apk del .build-deps  && \
#    rm -rf /var/cache/apk/* && rm -rf /usr/local/share/doc
    fi

# add custom wrapper for mira-ci
ADD sparco-ci.sh /usr/local/bin/

RUN set -eux ; \
    if [ "$DO_SPARCO" -eq "1" ] ; then \
    chmod +x /usr/local/bin/sparco-ci.sh && \
    ln -s /usr/local/bin/sparco-ci.sh /usr/local/bin/sparco-ci ; \
    fi

# TODO: remove patches to fix sparco plugin:
ADD sparco_patch/mira2_plugin_sparcomulti.i /opt/yorick/i/


###################################
#
#  Finalization
#

# Use valgrind with wisard:
ADD valgrind-wisard-ci.sh $WISARD_DIR/bin/

RUN set -eux ; \
    if [ $USE_VALGRIND -eq 1 ] ; then \
    apt-get update && apt-get install -y --no-install-recommends valgrind && \
    rm /usr/bin/wisard-ci && \
    ln -s $WISARD_DIR/bin/valgrind-wisard-ci.sh /usr/bin/wisard-ci && \
    echo "---> valgrind + wisard ready !" ; \
    fi


# Auto-deploy OImaging-uws.war on startup
# note: CONTEXT_PATH is not honored : app dir is the basename of the war (OImaging-uws here)
ADD OImaging-uws.war /usr/local/tomcat/webapps/

# Define USER to restrict privileges (not root) once image is production ready:
USER tomcat
EXPOSE 8080

