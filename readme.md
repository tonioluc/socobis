1-Palcer dans le même dossier que  Dokerfile et docker-compose.yml (dans -/socobis-prod)
    Pour mon cas : cd /home/antonio/ITU/S5/mr-tahina/socobis/socobis-prod

2- Construire l'image docker . Lancer la commande :
    docker compose build

3- Lancer le container docker :
    docker compose up

R.q : ilay container cocobis-wildfly matetika lané mialoha an.ilay oracle. Pour savoir que le container oracle est lancé , le terminal va afficher : "Database ready to use. Enjoy! ;)" . Laisse ce terminal là

4-Après lancement des container , on va créer l'utilisateur nécessaire dans oracle , il faut ouvrir une nouvelle fênetre terminal (n'importe où).
    Puis , lancer les commandes suivants :
        a-Entrer dans le shell du container oracle
            docker exec -it oracle-db bash
        b-Utiliser l'utilisateur existant :
            sqlplus system/oracle
        c-Puis on crée les utilsisateurs , donner les permissions nécessaires puis on sort du shell:
            CREATE USER socobis IDENTIFIED BY socobis;
            GRANT CONNECT, RESOURCE, DBA TO socobis;
            ALTER USER socobis DEFAULT TABLESPACE USERS;
            ALTER USER socobis TEMPORARY TABLESPACE TEMP;
            exit;
            exit;

5- Après la création de l'utilisateur dans oracle , on va passer à lîmportation des donnees initiales(encore dans un terminal n'importe où) :
    Lancer les commandes suivants :
        a-Copie  du fichier(socobis_20251107/socobis_20251107.dmp) des données dans le container oracle.
            *Pour mon cas (changer le chemin si nécessaire et mettre le chemin dans des double quote si votre chemin a d'espace):
                docker cp /home/antonio/ITU/S5/mr-tahina/socobis/socobis_20251107/socobis_20251107.dmp oracle-db:/home/oracle/
        b-Entrer dans le shell de oracle :
            docker exec -it oracle-db bash
        c-Importation des donnees :
            imp socobis/socobis file=/home/oracle/socobis_20251107.dmp full=yes ignore=yes

    R.q : L'importation va prendre un peu de temps , il va afficher à la fin une phrase comme :
        "Data importes successfullly with warning"
    On ignore ce warning.

6- Après l'importation des données. On va tester le projet s'il marche bien :
    a) Ouvrir le navigateur et entrer dans l'adresse :
        http://localhost:8080/socobis
        Vous devez voir la page de login 
    b) S'authentifier :
        Identifiant : admin
        Mot de passe : test
    
    R.q : Si vous êtes arriver su la page qui affiche : "Bienvenue sur votre espace de gestion" , le projet marche bien .
    R.q2 : Dans le terminal qu'on a laissé dans l'étape 3 qu'on peut voir le log du projet. Ce terminal va afficher probablement des erreur coloré en rouge mais oon peut ignorer cela car c'est l'erreur du assistant chatbot qui ne peut pas se connecter , voici le log par exemple :
        socobis-wildfly  | 04:15:59,561 INFO  [stdout] (default task-6) ADK Server for OCR NOT RUNNING, please consult Ms.Fits
        socobis-wildfly  | 04:15:59,562 ERROR [stderr] (default task-6) java.net.ConnectException: Connection refused (Connection refused)
    C'est un prpblème d'OCR qu'on peut ignoré totalement.

