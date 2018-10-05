# Use make to clean / build war file and create docker image:
make clean
make
# or make force-build (to clean cache)

# Run docker image:
docker images
docker run -p 8080:8080 <imageId>

# Access to docker container:
docker ps
docker exec -it <containerId> bash


TODO / Ideas:
idl-astro
https://github.com/wlandsman/IDLAstro

disable OpenMP:
-DOPENMP=ON|OFF

TODO: install astron (from github ?)

For a special compilation with all the options for fast code, including
targeting the machine's processor type, use for example:
-DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_FLAGS_RELEASE="-O3 -fno-math-errno
-fno-signaling-nans -msse2 -march=native -DNDEBUG"

nfft:
-O3 -fomit-frame-pointer -malign-double -fstrict-aliasing -ffast-math -march=native
