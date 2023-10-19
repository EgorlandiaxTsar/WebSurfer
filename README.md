# WebSurfer Search Engine
## Description
WebSurfer is a search engine made on Java with Spring Boot framework. It has 3 pages: Statistics, Management and Search. The statistics page is used to monitor the engine status, the management page is used to control the indexing process and the search page is used to search through all sites or, optionally, through one selected site. The indexing algorithm is based on lemma and indexes, it extracts text from pages and transforms it in lemmas, which, technically, are just normal word forms, then with lemmas and pages the algorithm creates indexes, which are the connections between pages and lemmas. The search algorithm uses these lemmas and indexes to find the most relevant results. WebSurfer is well optimized and uses multithreading to maximize the productivity. **NOTE**: this search engine works only with cyrillic letters. 
## Technologies
The following technologies were used in this project:
- Java (**v11.0.7**)
- Spring Boot (**v2.7.14**)
- Hibernate and Spring Data JPA
- Thymeleaf
- Lombok
- YAML
- ForkJoinPool API
- Lucene Morphology
- Jsoup
- Log4j2
## Use guide
### Dashboard
The dashboard is used to track statistics, engine status, errors and other general information.

![dashboard-snippet](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/dashboard-snippet.png?raw=true "Dahboard snippet")

You can watch every single site indexing state by clicking on it

![dashboard-indexing-site-state-example](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/dashboard-indexing-site-state-example.gif?raw=true "Dashboard indexing site state example")
### Management
The management is used to start/stop indexing and to add/update pages

![management-snippet](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/management-snippet.png?raw=true "Management snippet")

You can start indexing by using "Start Indexing" button

![management-start-indexing-example](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/management-start-indexing-example.gif?raw=true "Management start indexing exmaple")

You can stop indexing by using "Stop Indexing" button

![management-stop-indexing-example](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/management-stop-indexing-example.gif?raw=true "Management stop indexing exmaple")

You can add or update a page by filling the "Add/Update page" form and pressing "Add/Update" button

![management-add-or-update-page-example](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/management-add-or-update-page-example.gif?raw=true "Management add or update page example")
### Search
The search page is used to search across the sites

![search-snippet](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/search-snippet.png?raw=true "Search snippet")

You can also choose a specific site and the search will be executed only on that site

![search-by-site-example](https://github.com/EgorlandiaxTsar/WebSurferSnippetsStorage/blob/master/static/search-by-site-example.gif?raw=true "Search by site example")
## Installation guide
In this section there is a step-by-step guide to how to install WebSurfer locally
### MySQL installation
First of all, you need to set up a database:
- Go to the [installation link](https://dev.mysql.com/downloads/file/?id=520406) and press "No, thanks, just start my download" button
- Follow the MySQL installer steps
    - On first page choose `Full` installation type
    - Then press `Execute` and `Next`
    - In `Accounts and Roles` tab set the password for your database (**DO NOT LOOSE IT, YOU WILL NEED IT LATER**) 
    - Then press `Next`, `Next`, `Execute` and `Finish`
- Once MySQL is installed, open MySQL Workbench and connect through `Local instance MySQL80` by using yout password
- In the `Query 1` tab, opened by default, past and run the following script:
```SQL
CREATE DATABASE `websurfer` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT ENCRYPTION='N';
USE `websurfer`;
create table `index`
(
    id       int auto_increment
        primary key,
    page_id  int   not null,
    lemma_id int   not null,
    `rank`   float not null,
    constraint id_UNIQUE
        unique (id)
)
    engine = InnoDB;
USE `websurfer`;
create table lemma
(
    id        int auto_increment
        primary key,
    site_id   int          not null,
    lemma     varchar(255) not null,
    frequency int          not null,
    constraint id_UNIQUE
        unique (id)
)
    engine = InnoDB;
USE `websurfer`;
create table page
(
    id      int auto_increment
        primary key,
    site_id int        not null,
    path    text       not null,
    code    int        not null,
    content mediumtext not null,
    constraint id_UNIQUE
        unique (id)
)
    engine = InnoDB;
USE `websurfer`;
create table site
(
    id          int auto_increment
        primary key,
    status      enum ('INDEXING', 'INDEXED', 'FAILED') not null,
    status_time datetime                               not null,
    last_error  text                                   null,
    url         varchar(255)                           not null,
    name        varchar(255)                           not null,
    constraint id_UNIQUE
        unique (id)
)
engine = InnoDB;
```
- The script will create a database with required tables to launch the engine
### Server installation
- After database configuration, you can close MySQL Workbench
- Now, on the GitHub repository page download the project as .zip
- Unpack the project in any directory
- In the downloaded project folder, open file `bin/application.yaml`
- Change property `spring.datasource.username` to `root` and `spring.datasource.password` to `<your password chosen on the installation step>`
- Now run the server .exe file `websurfer_server.exe`

## Contacts
**Name:** Egor

**Instagram:** @egorgoncharov.e

**Telegram:** @egorlandiaxtsar
 