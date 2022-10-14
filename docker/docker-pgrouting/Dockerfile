# builder image
FROM postgis/postgis:15-3.3 AS pgrouting

# PG_MAJOR environment variable is made available by postgis image. It is used
# while building pgRouting extension.

ENV PGROUTING_VERSION 3.4.1
ENV PGROUTING_SHA256 a4e034efee8cf67582b67033d9c3ff714a09d8f5425339624879df50aff3f642

# - Build and install pgRouting from source.
# - ca-certificates as a dependency for wget is left installed in order to not
#   lose all CA certificates from the container.
RUN set -ex \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
    build-essential \
    ca-certificates \
    cmake \
    libboost-graph1.74.0 \
    libboost-graph1.74-dev \
    libcgal-dev \
    postgresql-server-dev-${PG_MAJOR} \
    wget \
 && wget -O pgrouting.tar.gz "https://github.com/pgRouting/pgrouting/archive/v${PGROUTING_VERSION}.tar.gz" \
 && echo "$PGROUTING_SHA256 *pgrouting.tar.gz" | sha256sum -c - \
 && mkdir -p /usr/src/pgrouting \
 && tar \
  --extract \
  --file pgrouting.tar.gz \
  --directory /usr/src/pgrouting \
  --strip-components 1 \
 && rm pgrouting.tar.gz \
 && cd /usr/src/pgrouting \
 && mkdir build \
 && cd build \
 && cmake .. \
 && make \
 && make install \
 && cd / \
 && rm -rf /usr/src/pgrouting \
 && apt-get purge -y --autoremove \
    build-essential \
    cmake \
    libboost-graph1.74.0 \
    libboost-graph1.74-dev \
    libcgal-dev \
    postgresql-server-dev-${PG_MAJOR} \
    wget \
 && apt-get clean -y \
 && rm -rf /var/lib/apt/lists/*

FROM pgrouting

RUN rm /docker-entrypoint-initdb.d/10_postgis.sh

HEALTHCHECK --interval=5s --timeout=5s --start-period=10s --retries=20 \
  CMD bash -c "pg_isready -h localhost -U \${POSTGRES_USER} -d \${POSTGRES_DB}"
