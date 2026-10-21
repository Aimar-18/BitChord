package com.music.bitchord.data.innertube

import com.music.bitchord.data.model.SearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchSuggestionsParserTest {

    /**
     * The shape YouTube Music actually returns: query completions in the first
     * section, playable entity rows in the second. The web client uses the
     * latter to surface a song after a short prefix; this parser must keep
     * both so the typeahead does not have to run a separate full search.
     */
    @Test
    fun keepsEntityRowsAlongsideQueryCompletions() {
        val response = Json.parseToJsonElement(
            """
            {
              "contents": [
                {
                  "searchSuggestionsSectionRenderer": {
                    "contents": [
                      {
                        "searchSuggestionRenderer": {
                          "suggestion": {
                            "runs": [{ "text": "te d" }, { "text": "ejo madrid shakira" }]
                          },
                          "navigationEndpoint": {
                            "searchEndpoint": { "query": "te dejo madrid shakira" }
                          }
                        }
                      }
                    ]
                  }
                },
                {
                  "searchSuggestionsSectionRenderer": {
                    "contents": [
                      {
                        "musicResponsiveListItemRenderer": {
                          "playlistItemData": { "videoId": "nIh6ENRECXM" },
                          "navigationEndpoint": {
                            "watchEndpoint": { "videoId": "nIh6ENRECXM" }
                          },
                          "flexColumns": [
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": {
                                  "runs": [{ "text": "Te Dejo Madrid" }]
                                }
                              }
                            },
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": {
                                  "runs": [
                                    { "text": "Song" },
                                    { "text": " • " },
                                    { "text": "Shakira" },
                                    { "text": " • " },
                                    { "text": "43M plays" }
                                  ]
                                }
                              }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent(),
        ).jsonObject

        val suggestions = InnertubeParser.parseSearchSuggestions(response)

        assertEquals(listOf("te dejo madrid shakira"), suggestions.queries)
        val song = (suggestions.rows.single() as SearchResult.Track).song
        assertEquals("nIh6ENRECXM", song.videoId)
        assertEquals("Te Dejo Madrid", song.title)
        assertEquals("Shakira", song.artist)
    }
}
