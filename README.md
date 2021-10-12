# Thematic Reasoner

The Thematic Reasoner is a tool able to deduce the main thematic subject of a collection of  entities (e.g. the artworks of a museum).
Specifically, by exploiting the alignments of an entity with DBpedia the tool is able to retrieve the main subject of an entity.
Then, the topological organization of the entities into zones is used to associate zones with thematic subjects.


## Example

Consider the following exhibition of the Hecht Museum. The exhibition involves 9 artworks localized in different 6 areas of the museum.
Each area is located within a room of the building (e.g. the "Bust of Hecht" is located in "Bust of Hecht" area which is an area of the "Entrance" room).

|        Room       |       Area      |           Artwork          |                              Wikipedia                             |
|:-----------------:|:---------------:|:--------------------------:|:------------------------------------------------------------------:|
| Entrance          | Bust of Hecht   | Bust of Hecht              | https://en.wikipedia.org/wiki/Reuben_Hecht                         |
| Galilee Rebellion |                 |                            |                                                                    |
|                   | Ventrina xx     | Arrows                     | https://en.wikipedia.org/wiki/Gamla                                |
|                   |                 | Catapult                   | https://en.wikipedia.org/wiki/Catapult                             |
|                   | Portraits       | Portrait of Joseph Flavius | https://en.wikipedia.org/wiki/Josephus                             |
|                   |                 | Portrait of Berniece       | https://en.wikipedia.org/wiki/Berenice_(daughter_of_Herod_Agrippa) |
|                   | Ventrina zz     | Roman war gear             | https://en.wikipedia.org/wiki/Legionary                            |
|                   |                 | Head of Vespian            | https://en.wikipedia.org/wiki/Vespasian                            |
| Main Hall         | Ventrina yy     | Bar Kochva Rebellion       | https://en.wikipedia.org/wiki/Bar_Kokhba_revolt                    |
| Corridor          | Jerusalem Photo | Jerusalem Photo            | https://en.wikipedia.org/wiki/Jerusalem                            |

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
