# Thematic Reasoner

The Thematic Reasoner is a tool able to deduce the main thematic subject of a collection of  entities (e.g. the artworks of a museum).
Specifically, by exploiting the alignments of an entity with DBpedia the tool is able to retrieve entity's main subjects.
Then, the topological organization of the entities into zones is used to associate zones with thematic subjects.

## Example

*TODO*

## Download and usage

An executable JAR can be obtained from the [Releases](https://github.com/spice-h2020/thematic.reasoner/releases) page.


The jar can be executed as follows:

```
usage: java -jar thematic.reasoner-ex-<version>.jar  -i path [-b uri] [-o
            filepath] [-s strategy]
 -b,--base-uri <uri>                    The namespace of the new URIs that
                                        will be created.
 -i,--input <path>                      The path to the file storing the
                                        input data about the collection of
                                        artworks to process.
 -o,--output-file <filepath>            The path to the output file
                                        [Default: out.ttl].
 -s,--output-strategy <[console|rdf]>   The output strategy [Default:
                                        console].
```

## License

The Thematic Reasoner is distributed under [Apache 2.0 License](LICENSE)
