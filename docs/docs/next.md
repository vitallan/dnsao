# Backlog

**DNSao** is still in the early stages of development, but to record the next ideas and planned steps for the tool, here’s what’s on the list:

* [Javalin](#remove-javalin)
* [Move Away from MkDocs](#move-away-from-mkdocs)

These are long term goals. At the moment, the focus is in the upstream forwarder features.

## Remove Javalin

UDP and TCP servers are created and managed directly within **DNSao**, but the web server was built using **Javalin**. There were several reasons for this choice, mainly to avoid spending time implementing an HTTP server in the early stages of the project.

Although **Javalin** is an excellent project and fits well with lightweight runtime goals, it makes more sense to have an internal HTTP server dedicated to serving metrics.

## Move Away from MkDocs

The worst tool for building a static site.

<div style="margin-bottom: 60px;"></div>
