# DOCKER COMMAND 

### docker build --tag sb-book-social-network . 

### docker image ls

### docker compose down

### docker build -t sb-book-network-app .   

### sudo kill -9 3018  

### sudo lsof -i :3306

### docker tag sb-book-social-network:latest sb-book-social-network:v1.0.0

### docker tag sb-book-social-network:v1.0.0 bangvo22/sb-book-social-network:v1.0.0

###  docker images

###  docker push bangvo22/sb-book-social-network:v1.0.0   

###  docker ps -a     

###  docker rmi f8488bc00528

###  docker rmi -f f8488bc00528    

docker run -dp 8600:8700 \
--name sb-book-social-network-container \
-v "$(pwd):/app" \
bangvo22/sb-book-social-network:v1.0.0


### docker network create sb-book-social-network-net 

docker run --rm -d \                                                        
-v mysql-sb-book-social-network:/var/lib/mysql \
-v mysql-sb-book-social-network-config-deamond:`/etc/mysql/conf.d` \
--name myql-sb-book-social-network \
-p 3310:3306 \
-e MYSQL_USER=admin -e MYSQL_PASSWORD=abc@#123 \
-e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=abc@#123 \
--network sb-book-social-network-net \
mysql:8.0.28


###  docker network rm  sb-book-social-network-net  

### docker network create sb-book-social-network-net     

###  mkdir -p /etc/mysql/conf.d   

###  sudo mkdir -p /etc/mysql/conf.d     

###  mkdir -p /etc/mysql/conf.d                                     


###  docker run -v mysql-sb-book-social-network-config-deamond:/etc/mysql/conf.d


docker run --rm -d \                                           
-v mysql-sb-book-social-network:/var/lib/mysql \
-v mysql-sb-book-social-network-config-deamond:`/etc/mysql/conf.d` \
--name myql-sb-book-social-network \
-p 3310:3306 \
-e MYSQL_USER=admin -e MYSQL_PASSWORD=abc@#123 \
-e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=abc@#123 \
--network sb-book-social-network-net \
mysql:8.0.28

# docker restart sb-book-social-network-container 

# docker logs sb-book-social-network-container   








