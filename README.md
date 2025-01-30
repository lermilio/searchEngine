OVERVIEW
NotGPT is a simulated search engine that processes web pages stored in .txt format, indexing their contents, ranking them by relevance, and returning results efficiently. This project was built using core data structures and algorithms, including B-Trees, HashMaps, BFS, and external sorting techniques to optimize ranking performance.



PROJECT GOALS 
*Implement a simplified web crawler to collect and process web pages.
*Design an indexing system using efficient data structures.
*Create a ranking algorithm that assigns an impact score to web pages.
*Implement a search function to return ranked results for a given query.



FEATURES

Indexing
*Extracts words and links from simulated web pages.
*Uses a B-Tree for URL-to-page mapping.
*Implements a HashMap to associate words with indexed pages.
*Ensures duplicate URLs and words are not re-indexed.

Ranking
*Assigns impact scores to pages based on incoming and outgoing links.
*Implements an iterative distribution process to refine impact scores.
*Uses external sorting to optimize impact calculations, reducing complexity from O(N+L) to O(L log L).

Searching
*Supports multi-word queries and retrieves relevant pages.
*Uses priority queues to rank and return the most relevant results.
*Implements iterator-based methods to efficiently compare page indices.



DATA STRUCTURE USED 
*B-Tree: Stores URL-to-index mappings for fast lookups.
*HashMap: Maps words to page indices for quick retrieval.
*Queue (LinkedList): Manages page processing using BFS.
*Set: Prevents duplicate indexing of URLs and words.
*Priority Queue: Ranks search results efficiently.



IMPLEMENTATION DETAILS

Indexing Web Pages
*indexPage(String url, String content): Maps a URL to an index and stores the extracted data.
*collect(String startURL): Uses BFS to traverse and index web pages.

Ranking Pages
*rankSlow(): Initial ranking method using multiple iterations.
*rankFast(): Optimized ranking with external sorting to handle large datasets.
*Fix for Zero-Link Pages: Ensures pages with no outgoing links fairly distribute their impact.

Searching for Queries
*search(String query): Retrieves and ranks pages matching the search terms.
*Helper Methods:
*allEqual(): Compares indices of matching pages.
*getNextPageIndices(): Iterates through stored page data.
*getLargest(): Finds the highest-ranked page.
