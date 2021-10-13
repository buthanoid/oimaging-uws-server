# Use make to clean / build war file and create docker image:
make clean
make
# or make force-build (to clean cache)

# Run local UWS server docker image:
make run

# Access to docker container:
docker ps
docker exec -it <containerId> bash


To enable/disable OpenMP:
-DOPENMP=ON|OFF


How to get gcc flags used by -march=native (cpu feature dependent) ?
    gcc -c -Q -march=native --help=target

