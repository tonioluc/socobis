# Socobis – Guide de démarrage rapide avec Docker

## Prérequis
- Docker et Docker Compose installés
- Le projet doit être placé dans le dossier socobis-prod (là où se trouvent le Dockerfile et le docker-compose.yml)

cd /home/antonio/ITU/S5/mr-tahina/socobis/socobis-prod
# Adaptez ce chemin selon votre configuration

### 1. Construire les images Docker
docker compose build

### 2. Lancer les conteneurs
docker compose up

Important : Laissez ce terminal ouvert.  
Le conteneur oracle-db démarre en premier. Attendez le message suivant dans les logs :
Database ready to use. Enjoy! ;)
Cela signifie qu’Oracle est prêt.

### 3. Créer l’utilisateur Oracle socobis

Ouvrez un nouveau terminal (n’importe où) et exécutez :

# a) Entrer dans le conteneur Oracle
docker exec -it oracle-db bash

# b) Se connecter avec l’utilisateur system
sqlplus system/oracle

# c) Créer l’utilisateur socobis avec les privilèges nécessaires
CREATE USER socobis IDENTIFIED BY socobis;
GRANT CONNECT, RESOURCE, DBA TO socobis;
ALTER USER socobis DEFAULT TABLESPACE USERS;
ALTER USER socobis TEMPORARY TABLESPACE TEMP;

# Quitter SQL*Plus puis le conteneur
exit;
exit;

### 4. Importer les données initiales

Toujours dans un terminal (n’importe où) :

# a) Copier le dump dans le conteneur Oracle
# Adaptez le chemin source si nécessaire (entourez de guillemets si espaces)
docker cp "/home/antonio/ITU/S5/mr-tahina/socobis/socobis_20251107/socobis_20251107.dmp" oracle-db:/home/oracle/

# b) Re-entrer dans le conteneur
docker exec -it oracle-db bash

# c) Importer les données
imp socobis/socobis file=/home/oracle/socobis_20251107.dmp full=yes ignore=yes

L’import prend plusieurs minutes. À la fin, vous verrez quelque chose comme :
Import terminated successfully with warnings.
Les warnings peuvent être ignorés.

Quittez le conteneur :
exit

### 5. Tester l’application

Ouvrez votre navigateur et allez à :

http://localhost:8080/socobis

Vous devez voir la page de connexion.

#### Identifiants de test
- Login : admin  
- Mot de passe : test

Si vous arrivez sur la page avec le message « Bienvenue sur votre espace de gestion », l’application fonctionne parfaitement !

### Remarques
- Le terminal de l’étape 2 (où vous avez lancé docker compose up) affiche les logs de WildFly.
- Vous verrez probablement des erreurs rouges comme :
  ADK Server for OCR NOT RUNNING, please consult Ms.Fits
  java.net.ConnectException: Connection refused
  → C’est normal : il s’agit du service OCR/chatbot qui n’est pas démarré. Vous pouvez les ignorer totalement.

Socobis est maintenant opérationnel ! Enjoy ;)

07/12/2025:30;10/12/2025:60;15/12/2025:10