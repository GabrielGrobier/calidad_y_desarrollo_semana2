FROM httpd:2.4

RUN apt-get update \
    && apt-get install -y --no-install-recommends openssl \
    && rm -rf /var/lib/apt/lists/*

COPY server.key /usr/local/apache2/conf/server.key
COPY server.crt /usr/local/apache2/conf/server.crt
COPY httpd.conf /usr/local/apache2/conf/httpd.conf
COPY httpd-ssl.conf /usr/local/apache2/conf/extra/httpd-ssl.conf
COPY frontend/ /usr/local/apache2/htdocs/

EXPOSE 80 443
CMD ["httpd-foreground"]
