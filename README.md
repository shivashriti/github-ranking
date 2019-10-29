## GitHub Ranking

Github ranking system provides an endpoint that given the name of the organization returns a list of contributors sorted by the number of contributions.
The backend uses`akka-http` for HTTP based services & client calls and `akka-stream` for stream processing and handling backpressure.


### API
`GET    /org/<org-name>/contributors`


### Quick Start 

#### Running service with sbt

- set environment variable `GH_TOKEN` to handle GitHub’s API rate limit restriction

    `export GH_TOKEN=<insert-your-token-here>`

- On root of the project, run `sbt run`

    `~/myWorkspace/github-ranking 👉 $sbt run`

*This will start the server at port **8080**.*


#### Test

- To run unit tests, run `sbt test`

    `~/myWorkspace/github-ranking 👉 $sbt test`