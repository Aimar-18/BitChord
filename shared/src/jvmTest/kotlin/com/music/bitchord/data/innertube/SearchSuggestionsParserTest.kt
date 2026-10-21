package com.music.bitchord.data.innertube

import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.hasPlayableTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertTrue(suggestions.rows.hasPlayableTrack())
    }

    /**
     * Signed-in suggestions can answer with an artist card and a music video
     * while offering no audio track. The parser keeps the artist and drops the
     * video (the mixed page is music-only), so the pipeline must not mistake
     * the non-empty answer for a complete one: it still needs the full-search
     * fallback that finds the catalogue track.
     */
    @Test
    fun browseOnlySuggestionsNeedTheFullSearchFallback() {
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
                            "runs": [{ "text": "en tus pupilas shakira" }]
                          },
                          "navigationEndpoint": {
                            "searchEndpoint": { "query": "en tus pupilas shakira" }
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
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UCo6JijJGA3IvIiPsawDK3Ww",
                              "browseEndpointContextSupportedConfigs": {
                                "browseEndpointContextMusicConfig": {
                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                }
                              }
                            }
                          },
                          "flexColumns": [
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": { "runs": [{ "text": "Shakira" }] }
                              }
                            },
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": { "runs": [{ "text": "Artist" }] }
                              }
                            }
                          ]
                        }
                      },
                      {
                        "musicResponsiveListItemRenderer": {
                          "playlistItemData": { "videoId": "X5KhkTU0AEI" },
                          "flexColumns": [
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": {
                                  "runs": [{ "text": "Shakira - En Tus Pupilas" }]
                                }
                              }
                            },
                            {
                              "musicResponsiveListItemFlexColumnRenderer": {
                                "text": {
                                  "runs": [
                                    { "text": "Vídeo" },
                                    { "text": " • " },
                                    { "text": "47 K visualizaciones" }
                                  ]
                                }
                              }
                            }
                          ],
                          "thumbnail": {
                            "musicThumbnailRenderer": {
                              "thumbnail": {
                                "thumbnails": [
                                  {
                                    "url": "https://example.invalid/video.jpg",
                                    "width": 400,
                                    "height": 225
                                  }
                                ]
                              }
                            }
                          }
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

        assertEquals(listOf("en tus pupilas shakira"), suggestions.queries)
        assertTrue(suggestions.rows.single() is SearchResult.Browse)
        assertFalse(suggestions.rows.hasPlayableTrack())
    }
}
