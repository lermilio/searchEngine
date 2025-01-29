package prog11;

import prog08.ExternalSort;
import prog09.BTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.SQLOutput;
import java.util.*;

import static java.lang.System.out;

public class NotGPT implements SearchEngine{

    public HardDisk pageDisk = new HardDisk();

    Map<String, String> indexOfURL = new BTree(100);

    public HardDisk wordDisk = new HardDisk();

    HashMap<String, Long> indexOfWord = new HashMap<String,Long>();

    public class Vote implements Comparable<Vote>{

        public Long index;
        public double vote;

        /**
         * @param o the object to be compared.
         * @return
         */
        @Override
        public int compareTo(Vote o) {

            if(index.compareTo(o.index) != 0){
                return Long.compare(index, o.index);
            }else{
                return Double.compare(vote, o.vote);
            }
        }

        public String toString(){
            return index + " " + vote;
        }

    }

    class VoteScanner implements ExternalSort.EScanner<Vote> {
        class Iter implements Iterator<Vote> {
            Scanner in;

            Iter (String fileName) {
                try {
                    in = new Scanner(new File(fileName));
                } catch (Exception e) {
                    out.println(e);
                }
            }

            public boolean hasNext () {
                return in.hasNext();
            }

            public Vote next () {
                Vote v = new Vote();

                v.index = in.nextLong();
                v.vote = in.nextDouble();

                return v;
            }
        }

        public Iterator<Vote> iterator (String fileName) {
            return new VoteScanner.Iter(fileName);
        }
    }


    public long indexWord(String word){
        Long index = wordDisk.newFile();
        InfoFile info = new InfoFile(word);
        wordDisk.put(index, info);
        indexOfWord.put(word, index);
        out.println("indexing word "+ word +" index "+ index +" file " + info);
        return index;
    }

    Long indexPage(String url){
        Long index = pageDisk.newFile();
        InfoFile info = new InfoFile(url);
        pageDisk.put(index, info);
        indexOfURL.put(url, index.toString());
        String message = String.format("indexing url %s index %s file %s", url,index, info);
        out.println(message);
        return index;
    }

    @Override
    public void collect(Browser browser, List<String> startingURLs) {
        String message = String.format("starting pages %s", startingURLs);
        out.println(message);
        ArrayDeque<Long> que = new ArrayDeque<Long>();
        for(String url: startingURLs){
            if(!indexOfURL.containsKey(url)){
               Long index = indexPage(url);
               que.add(index);
            }
        }
        while(!que.isEmpty()){
            String message2 = String.format("queue %s", que);
            out.println(message2);
            Long index = que.poll();
            String message3 = String.format("dequeued %s", pageDisk.get(index));
            out.println(message3);
            InfoFile info = pageDisk.get(index);
            String url = info.data;
            if(browser.loadPage(url)){
                List<String> urlList = browser.getURLs();
                String message4 = String.format("urls %s", urlList);
                out.println(message4);
                Set<String> collection = new HashSet<String>();
                for(String x: urlList){
                    if(!indexOfURL.containsKey(x)){//has this URL been indexed already
                        Long indexOfUrl = indexPage(x);
                        que.add(indexOfUrl);
                    }
                    if(!collection.contains(x)){
                        collection.add(x);
                        info.indices.add(Long.parseLong(indexOfURL.get(x)));
                    }
                }
                String x = String.format("updated page file %s", info);
                out.println(x);
                List<String> words = browser.getWords();
                Set<String> collection2 = new HashSet<String>();
                String message5 = String.format("words %s", words);
                out.println(message5);

                for(String word: words){
                    if(!indexOfWord.containsKey(word)){//has this URL been indexed already
                        indexWord(word);
                    }
                    InfoFile info2 = wordDisk.get(indexOfWord.get(word));
                    if(!collection2.contains(word)){
                        collection2.add(word);
                        info2.indices.add(index);
                        String message6 = String.format("updated word %s index %s file %s", word, indexOfWord.get(word), info2);
                        out.println(message6);
                    }
                }
            }
        }
    }

    void rankSlow() {

        double zeroLinkImpact = 0.0;

        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            double impactPerIndex = file.impact / file.indices.size();
            if (file.indices.isEmpty()) {
                zeroLinkImpact += file.impact;
            }
            for (long indexOnPage : file.indices) {
                file = pageDisk.get(indexOnPage);
                file.impactTemp += impactPerIndex;
            }
        }

        zeroLinkImpact = zeroLinkImpact / pageDisk.entrySet().size();

        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.impact = file.impactTemp + zeroLinkImpact;
            file.impactTemp = 0.0;
        }
    }

    void rankFast() {
        double zeroLinkImpact = 0.0;

        try {
            PrintWriter out = new PrintWriter("unsorted-votes.txt");
            for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
                long index = entry.getKey();
                InfoFile file = entry.getValue();
                double impactPerIndex = file.impact / file.indices.size();
                if (file.indices.isEmpty()) {
                    zeroLinkImpact += file.impact;
                }
                for (long indexOnPage : file.indices) {
                    Vote vote = new Vote();
                    vote.vote = impactPerIndex;
                    vote.index = indexOnPage;
                    out.println(vote);
                }
                out.close();

                ExternalSort sorter = new ExternalSort<Vote>(new VoteScanner());

                sorter.sort("unsorted-votes.txt", "sorted-votes.txt");
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        VoteScanner voteScanner = new VoteScanner();
        Iterator<Vote> iter = voteScanner.iterator("sorted-Votes.txt");

        zeroLinkImpact = zeroLinkImpact / pageDisk.entrySet().size();

        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();

            while(iter.hasNext()){
                if(index == iter.next().index){
                    file.impact += iter.next().vote + zeroLinkImpact;
                    iter.next();
                }
                else{
                    break;
                }
            }
        }
    }


    @Override
    public void rank(boolean fast) {
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.impact = 1.0;
            file.impactTemp = 0.0;
        }
        if(fast){
            for(int i = 0; i<20; i++){
                rankFast();
            }
        }
        else{
            for(int i = 0; i<20; i++){
                rankSlow();
            }
        }
    }

    private boolean allEqual (long[] array){
        for(int i = 0; i < array.length - 1; i++){
            if(array[i] != array[i+1]){
                return false;
            }
        }
        return true;
    }

    private long getLargest (long[] array){
        long largest = 0;
        for(int i = 0; i < array.length; i++){
            if(array[i] > largest){
                largest = array[i];
            }
        }
        return largest;
    }

    private boolean getNextPageIndices (long[] currentPageIndices, Iterator<Long>[] pageIndexIterators) {
        if(allEqual(currentPageIndices)){
            for(int i = 0; i < currentPageIndices.length; i++){
                if(!pageIndexIterators[i].hasNext()){
                    return false;
                }
                else {
                    currentPageIndices[i] = pageIndexIterators[i].next();
                }
            }
        }
        else{
            long largest = getLargest(currentPageIndices);
            for(int i = 0; i < currentPageIndices.length; i++){
                if(currentPageIndices[i] != largest && !pageIndexIterators[i].hasNext()){
                    return false;
                }
                if(currentPageIndices[i] !=  largest){
                    currentPageIndices[i] = pageIndexIterators[i].next();
                }
            }
        }
        return true;
    }

    class PageIndexComparator implements Comparator<Long> {

        @Override
        public int compare(Long pageIndex1, Long pageIndex2) {
            return Double.compare(pageDisk.get(pageIndex1).impact, pageDisk.get(pageIndex2).impact);
        }
    }

    @Override
    public String[] search(List<String> searchWords, int numResults) {
        Iterator<Long>[] pageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        long[] currentPageIndices = new long[searchWords.size()];
        PriorityQueue<Long> bestPageIndices;
        bestPageIndices = new PriorityQueue<Long>(new PageIndexComparator());
        for(int i = 0; i < searchWords.size(); i++){
            String word = searchWords.get(i);
            long index = indexOfWord.get(word);
            InfoFile file = wordDisk.get(index);
            pageIndexIterators[i] = file.indices.iterator();
        }

        while(getNextPageIndices(currentPageIndices, pageIndexIterators)){
            if(allEqual(currentPageIndices)){
                long index = currentPageIndices[0];
                InfoFile file = pageDisk.get(index);
                String url = file.data;
                out.println(url);
                if(bestPageIndices.size() != numResults){
                    bestPageIndices.offer(index);
                }
                else{
                    if(pageDisk.get(bestPageIndices.peek()).impact < file.impact){
                        bestPageIndices.poll();
                        bestPageIndices.offer(index);
                    }
                }
            }
        }
        String[] results = new String[bestPageIndices.size()];
        Stack<Long> temp = new Stack<Long>();
        while(!bestPageIndices.isEmpty()){
            temp.add(bestPageIndices.poll());
        }
        int count = 0;
        while(!temp.isEmpty()){
            results[count] = pageDisk.get(temp.pop()).data;
            count++;
        }
        return results;
    }
}
