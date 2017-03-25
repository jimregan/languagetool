/* LanguageTool, a natural language style checker 
 * Copyright (C) 2010 Daniel Naber (http://www.languagetool.org)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.dev.conversion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryIterator;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.English;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.patterns.PatternToken;
import org.languagetool.rules.patterns.PatternRule;
import org.languagetool.rules.patterns.AbstractPatternRule;
import org.languagetool.rules.patterns.PatternRuleLoader;

public class RuleCoverage {

    private JLanguageTool tool;    
    private DictionaryIterator dictIterator;
    private DictionaryLookup dictLookup;
    private Language language;
    private String filename;
    private Path dictFile;
    
    private String ruleFileHeader = RuleConverter.xmlHeader;
    private String categoriesString = "<category name=\"test\">";
    private String endCategoriesString = "</category>";
    private String endRulesString = "</rules>"; 
    
    private static Pattern regexSet = Pattern.compile("^\\[([^\\-])*?\\]$");

    // default constructor; defaults to English
    public RuleCoverage() throws IOException {
      language = new English();
      tool = new JLanguageTool(language);
        tool.disableRule("UPPERCASE_SENTENCE_START");
        tool.disableRule("EN_UNPAIRED_BRACKETS");
        tool.disableRule("EN_A_VS_AN");
        setupDictionaryFiles();
    }
    
    // disable some of the default rules in the constructors
    //TODO: disable the right rules for each language
    // though this matters less when we return an array of all covering rules
    public RuleCoverage(Language language) throws IOException {
      this.language = language;
      tool = new JLanguageTool(language);
        setupDictionaryFiles();
    }
    
    // for testing purposes, defaults to English
    public RuleCoverage(String dictFileName) throws IOException {
      language = new English();
      tool = new JLanguageTool(language);
        tool.disableRule("UPPERCASE_SENTENCE_START");
        tool.disableRule("EN_UNPAIRED_BRACKETS");
        tool.disableRule("EN_A_VS_AN");
        this.filename = dictFileName;
        this.dictFile = Paths.get(filename);
        setupDictionaryFiles();
    }
    
    public JLanguageTool getLanguageTool() {
      return tool;
    }

    // not really used anymore
    public void evaluateRules(String grammarfile) throws IOException {
        List<AbstractPatternRule> rules = loadPatternRules(grammarfile);
        for (AbstractPatternRule rule : rules) {
            String example = generateIncorrectExample(rule);
            System.out.println("Rule " + rule.getId() + " is covered by " + isCoveredBy(example) + " for example " + example);
        }
    }
    
    // not really used anymore
    public void splitOutCoveredRules(String grammarfile, String discardfile) throws IOException {
      List<AbstractPatternRule> rules = loadPatternRules(grammarfile);
      
      PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(grammarfile),"UTF-8"));
      PrintWriter w2 = null;
      int discardedRules = 0;
      
        
      for (AbstractPatternRule arule : rules) {
        PatternRule rule = (PatternRule) arule;
        String example = generateIncorrectExample(rule);
        if (isCoveredBy(example) == null) {
          w.write(rule.toXML());
        } else {
          if (w2 == null) {
            w2 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(discardfile),"UTF-8")); 
          }
          discardedRules++;
          w2.write(rule.toXML());
        }
      }
      
      if (discardedRules > 0) {
        System.out.println(Integer.toString(discardedRules) + " rules already covered, written to " + discardfile);
      }
      w.close();
      if (w2 != null) {
        w2.close();
      }
    }
    
    /**
     * Returns true if the input string is covered by an existing JLanguageTool error 
     * @param str input error string
     * @return true if (entire) string is considered an error, false o.w.; this doesn't work
     */
    public boolean isCovered(String str) throws IOException {
        List<RuleMatch> matches = tool.check(str);
        return (matches.size() > 0);        
    }
    
    /**
     * Returns a list of covering rules for the given example string
     */
    public String[] isCoveredBy(String str) throws IOException {
      List<RuleMatch> matches = tool.check(str);
      ArrayList<String> coverages = new ArrayList<>();
      if (matches.size() > 0) {
        for (RuleMatch match : matches) {
          coverages.add(match.getRule().getId());
        }
      }
      return coverages.toArray(new String[coverages.size()]);
    }
    
    public String[] isCoveredBy(AbstractPatternRule rule) throws IOException {
      ArrayList<String> coverages = new ArrayList<>();
      String example = generateIncorrectExample(rule);
    List<RuleMatch> matches = tool.check(example);
    if (matches.size() > 0) {
        for (RuleMatch match : matches) {
          coverages.add(match.getRule().getId());
        }
      }
      return coverages.toArray(new String[coverages.size()]);
    }
    
    public ArrayList<String[]> isCoveredBy(List<AbstractPatternRule> rules) throws IOException {
      ArrayList<String[]> coverages = new ArrayList<>();
      for (AbstractPatternRule rule : rules) {
        String[] cov = isCoveredBy(rule);
        coverages.add(cov);
      }
      return coverages;
    }
    
    /**
     * Generates an error string that matches the given AbstractPatternRule object 
     */
    public String generateIncorrectExample(AbstractPatternRule patternrule) {
        ArrayList<String> examples = new ArrayList<>();
        List<PatternToken> patternTokens = patternrule.getPatternTokens();
        for (int i=0;i< patternTokens.size();i++) {
          List<PatternToken> prevExceptions;
          if (i == patternTokens.size()-1) {
            prevExceptions = new ArrayList<>();
          } else {
            prevExceptions = patternTokens.get(i+1).getPreviousExceptionList();
            if (prevExceptions == null) prevExceptions = new ArrayList<>();
          }
            examples.add(getSpecificExample(patternTokens.get(i),prevExceptions, patternTokens,examples));
        }
        // it's okay to not deal with apostrophes as long as we turn off the unpaired brackets rule, for English at least
        StringBuilder sb = new StringBuilder();
        //TODO: doesn't deal with spacebefore=no
        for (String example : examples) {
          sb.append(example).append(" ");
        }
        String s = sb.toString().replaceAll("\\ \\.\\ ", "").trim();  // to fix the period problem 
        return s;
    }
    
    // Not using this method yet
//    public String generateCorrectExample(PatternRule patternrule) {
//      String incorrectExample = generateIncorrectExample(patternrule);
//      AnalyzedSentence analyzedSentence = null;
//      try {
//        analyzedSentence = tool.getAnalyzedSentence(incorrectExample);
//        RuleMatch[] ruleMatches = patternrule.match(analyzedSentence);
//        for (RuleMatch rm : ruleMatches) {
//          patternrule.addRuleMatch(rm);
//        }
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//
//      ArrayList<String> examples = new ArrayList<String>();
//      List<Match> matches = patternrule.getSuggestionMatches();
//      ArrayList<Element> elements = new ArrayList<Element>();
//      for (Match m : matches) {
//        int ref = m.getTokenRef();
//        Element refElement = patternrule.getElements().get(ref);
//        elements.add(refElement);
//      }
//      for (int i=0;i<elements.size();i++) {
//          List<Element> prevExceptions;
//          if (i == elements.size()-1) {
//            prevExceptions = new ArrayList<Element>();
//          } else {
//            prevExceptions = elements.get(i+1).getPreviousExceptionList();
//            if (prevExceptions == null) prevExceptions = new ArrayList<Element>();
//          }
//            examples.add(getSpecificExample(elements.get(i),prevExceptions,elements,examples));
//        }
//        // it's okay to not deal with apostrophes as long as we turn off the unpaired brackets rule, for English at least
//        StringBuilder sb = new StringBuilder();
//        //TODO: doesn't deal with spacebefore=no
//        for (String example : examples) {
//          sb.append(example + " ");
//        }
//        String s = sb.toString().replaceAll("\\ \\.\\ ", ".").trim();  // to fix the period problem 
//        return s;
//    }
//    
    
    /**
     * Generates a word that matches the given Element 
     */
    //TODO: doesn't deal with skipped tokens
    @SuppressWarnings("unchecked")
  public String getSpecificExample(PatternToken e, List<PatternToken> prevExceptions, List<PatternToken> patternTokens, ArrayList<String> examples) {
        // if this is part of (the first of) a list of and-ed tokens
      if (e.hasAndGroup()) {
          List<PatternToken> andGroup = e.getAndGroup();
          andGroup.add(e); // add the token itself to the and group, so we can process them together
          // still, if one of the tokens in the and group is just a (non-regexp) token, we can return that as the example
          for (PatternToken and : andGroup) {
            if (isJustToken(and)) {
              return and.getString();
            }
            if (isPunctuation(and)) {
              return getOnePunc(and);
            }
          }
          // get the patterns of all the and-ed elements, to make processing faster
          List<Pattern> tokenPatterns = new ArrayList<>(andGroup.size());
          List<Pattern> posPatterns = new ArrayList<>(andGroup.size());
          // get all the exceptions and attributes
          List<PatternToken> allExceptions = new ArrayList<>();
          allExceptions.addAll(prevExceptions);  // add all the exceptions from the next token with scope="previous"
          for (int a=0;a<andGroup.size();a++) {
            PatternToken and = andGroup.get(a);
            List<PatternToken> ex = and.getExceptionList();
            if (ex != null) {
              allExceptions.addAll(and.getExceptionList());
            }
            if (and.isReferenceElement()) {
              and = getReferenceElement(and, patternTokens,examples);  // gets the string for the element if it's a match token
            }
            String andPostag = and.getPOStag();
            String andToken = and.getString();
            tokenPatterns.add(Pattern.compile(andToken));
            if (andPostag != null) {
              if (and.isPOStagRegularExpression()) {
                posPatterns.add(Pattern.compile(andPostag));
              } else {
                posPatterns.add(Pattern.compile(Pattern.quote(andPostag)));
              }
              
            } else {
              posPatterns.add(null);
            }
            andGroup.set(a,and);
          }
          // get exceptions in attribute form for faster processings
          List<List<Pattern>> exceptionAttributes = getExceptionAttributes(allExceptions);
          
          // do the dictionary iteration thing; this part could take a while, depending on how far through the dict we have to go
          int numResets = 0;
            while (numResets < 2) {
              if (!dictIterator.hasNext()) {
                dictIterator = resetDictIterator();
                numResets++;
              }
                String word = dictIterator.next().getWord().toString();
                // check if the word meets all the and-ed criteria
                boolean matched = true;
                for (int i=0;i<andGroup.size();i++) {
                  if (!isExampleOf(word, tokenPatterns.get(i), posPatterns.get(i), andGroup.get(i))) {
                    matched = false;
                    break;
                  }
                }
                if (matched) {
                  if (!inExceptionList(word, exceptionAttributes, allExceptions)) {
                    return word;
                  }
                } 
            } 
        } 
      // just a single (non-and-ed) token
      else {
        if (e.isReferenceElement()) {
          e = getReferenceElement(e, patternTokens, examples);
        }
          String token = e.getString();
          String postag = e.getPOStag();
            List<PatternToken> exceptions = e.getExceptionList();
            if (exceptions == null) {
              exceptions = new ArrayList<>();
            }
            exceptions.addAll(prevExceptions);
            
            List<List<Pattern>> exceptionAttributes = getExceptionAttributes(exceptions);

            if (e.isSentenceStart()) {
                return "";
            }
            // <token>word</token>
            if (isJustToken(e)) {
                return token;
            }
            if (isPunctuation(e)) {
          return getOnePunc(e);
        }
            
            // need smarter example generation, especially for simple or-ed lists of words. 
            if (isSimpleOrRegex(e)) {
              // pick an element from the or-ed list at random
              return randomOredElement(e);
            }
            
            Pattern tokenPattern = Pattern.compile(token);
            Pattern posPattern;
            if (postag != null) {
              if (e.isPOStagRegularExpression()) {
                posPattern = Pattern.compile(postag);
              } else {
                posPattern = Pattern.compile(Pattern.quote(postag));
              }
              
              if (postag.equals("SENT_END")) {
                posPattern = null;
              }
              
            } else {
              posPattern = null;
            }
            
            // only allows approx. one pass through the dictionary
            int numResets = 0;
            while (numResets < 2) {
              if (!dictIterator.hasNext()) {
                dictIterator = resetDictIterator();
                numResets++;
              }
                String word = dictIterator.next().getWord().toString();
                if (isExampleOf(word, tokenPattern, posPattern, e) &&
                  !inExceptionList(word, exceptionAttributes, exceptions)) {
                    return word;
                }
            } 
        }
   
        return null;  // if no example can be found
    }
    
    /**
     * Returns an element with the string set as the previously matched element
     */
    private PatternToken getReferenceElement(PatternToken e, List<PatternToken> patternTokens, ArrayList<String> examples) {
      int r = e.getMatch().getTokenRef();
      PatternToken newPatternToken = new PatternToken(examples.get(r), patternTokens.get(r).isCaseSensitive(), false, false);
      newPatternToken.setNegation(e.getNegation());
      return newPatternToken;
      
    }
    
    /**
     * Gets all the attributes of each element of the exception, so we don't have to keep compiling the Pattern,
     * which wastes a lot of time
     */
    @SuppressWarnings("unchecked")
  private List<List<Pattern>> getExceptionAttributes(List<PatternToken> exceptions) {
      if (exceptions.size() == 0) {
        return new ArrayList<>();
      } 
      int size = exceptions.size();
      List<List<Pattern>> ret = new ArrayList<>(6);
      List<Pattern> tokenPatterns = new ArrayList<>(size);
      List<Pattern> posPatterns = new ArrayList<>(size);
      for (PatternToken e : exceptions) {
        String token = e.getString();
        String postag = e.getPOStag();
        Pattern tokenPattern = Pattern.compile(token);
        Pattern posPattern;
            if (postag != null) {
              posPattern = Pattern.compile(postag);
            } else {
              posPattern = null;
            }
            
            tokenPatterns.add(tokenPattern);
            posPatterns.add(posPattern);
            
      }
      ret.add(tokenPatterns);
      ret.add(posPatterns);
      return ret;
    }
    
    /**
     * Returns a random one of the or-ed elements. Random seems like the right thing to do here.
     * Only applied to simple or-ed lists of words, e.g. this|that|those
     */
    private String randomOredElement(PatternToken e) {
      String[] split = e.getString().split("\\|");
      Random rng = new Random();
      int index = rng.nextInt(split.length);
      return split[index];
    }
    
    /** 
     * Faster version of inExceptionList, because we don't have to re-compile the Patterns for the exception elements
     */
    @SuppressWarnings("unchecked")
  private boolean inExceptionList(String word, List<List<Pattern>> exceptionAttributes, List<PatternToken> exceptions) {
      if (exceptions.size() == 0) {
        return false;
      }
      List<Pattern> tokenPatterns = exceptionAttributes.get(0);
      List<Pattern> posPatterns = exceptionAttributes.get(1);
      
      for (int i=0;i<exceptions.size();i++) {
        PatternToken curException = exceptions.get(i);
        if (isExampleOf(word,tokenPatterns.get(i),
            posPatterns.get(i),
            curException)) {
          return true;
        }
      }
      return false;
    }
    
    
    /**
     * Faster version of isExampleOf, since you don't have to recompile the Patterns every time
     */
    public boolean isExampleOf(String word, Pattern tokenPattern, Pattern posPattern, PatternToken e) {
      if (tokenPattern.pattern().isEmpty() && posPattern == null) {
          return true;
        }
      boolean tokenMatches = true;
        boolean postagMatches = false;
        boolean isTokenEmpty = e.getString().isEmpty();
        boolean hasPosTag = (posPattern != null);
        boolean negate = e.getNegation();
        boolean postagNegate = e.getPOSNegation();
        boolean inflected = e.isInflected();
        
        if (posPattern == null) {
          postagMatches = true;
        }
        if (!isTokenEmpty) {
          Matcher m;
          boolean matches = false;
          // checking inflected matches
          if (inflected) {
            if (isInflectedStringMatch(word,e)) {
              matches = true;
            }
          } else {
            m = tokenPattern.matcher(word);
            if (m.matches()) matches = true;
          }
            
            if (matches) {
                if (negate) {
                    tokenMatches = false; 
                }
            } else {
                if (!negate) {
                    tokenMatches = false;
                }
            }
        }
        if (hasPosTag) {
            List<String> postags = getPosTags(word);
            for (String s : postags) {
                Matcher m = posPattern.matcher(s);
                if (m.matches()) {
                    if (!postagNegate) {
                        postagMatches = true;
                        break;
                    }
                } else {
                    if (postagNegate) {
                        postagMatches = true;
                        break;
                    }
                }
            }
            if (postags.size() == 0) {
                postagMatches = false;
            }
            
        }
        return (tokenMatches && postagMatches);
    }
    
    private boolean isInflectedStringMatch(String word, PatternToken e) {
      Matcher m;
      Pattern lemmaPattern = Pattern.compile(RuleConverter.glueWords(getLemmas(e)));
    List<String> wordLemmas = getLemmas(word);
    for (String lemma : wordLemmas) {
      m = lemmaPattern.matcher(lemma);
      if (m.matches()) {
        return true;
      }
    }
    return false;
    }
    
    /**
     * Returns a list of the word's POS tags
     */
    private List<String> getPosTags(String word) {
        List<WordData> lwd = dictLookup.lookup(word);
        ArrayList<String> postags = new ArrayList<>();
        for (WordData wd : lwd) {
            postags.add(wd.getTag().toString());
        }
        return postags;
    }
    /**
     * Returns an or-ed group of the lemmas of a word
     */
    private ArrayList<String> getLemmas(String word) {
      List<WordData> lwd = dictLookup.lookup(word);
      ArrayList<String> lemmas = new ArrayList<>();
      for (WordData wd : lwd) {
        if (!lemmas.contains(wd.getStem())) {
          lemmas.add(wd.getStem().toString());
        }
      }
      return lemmas;
    }
    
    // returns the lemmas of an element; 
    // the point of this method is that so we can get the lemmas of a bunch of or-ed words
    private ArrayList<String> getLemmas(PatternToken e) {
      if (!e.isRegularExpression()) {
        return getLemmas(e.getString());
      } else {
        if (isOrRegex(e)) {
          ArrayList<String> lemmas = new ArrayList<>();
          String[] words = e.getString().split("\\|");
          for (String word : words) {
            lemmas.addAll(getLemmas(word));
          }
          return lemmas;
        }
        return null;
      }
    }
    
    
    /**
     * Returns true if the element has a (non-regexp, non-negated) token and no exception list
     */
    private static boolean isJustToken(PatternToken e) {
      return (!e.getString().isEmpty() && !e.isRegularExpression() && !e.getNegation() && e.getExceptionList() == null);
    }
    
    /**
     * Returns true if the given element's string is a regex set of punctuation.
     * e.g. ['"] or [.,;:?!]
     */
    public static boolean isPunctuation(PatternToken e) {
      if (regexSet.matcher(e.getString()).matches() && !e.getNegation() && e.getPOStag() == null) {
        return true;
      }
      return false;
    }
    
    /**
     * Grabs the first element of a punctuation set matched by the above method.
     */
    public String getOnePunc(PatternToken e) {
      String set = e.getString();
      Matcher m = regexSet.matcher(set);
      m.find();
      return m.group(1);
    }
    
    /** 
     * Returns true if the element is an or-ed list of words, without a specified pos-tag.
     * e.g. can|could|would|should
     */
    private static boolean isSimpleOrRegex(PatternToken e) {
      // any number of conditions that could halt this check
      if (e.getString().isEmpty()) return false;
      if (e.getPOStag() != null) return false;
      if (e.getNegation()) return false;
      if (!e.isRegularExpression()) return false;
      if (e.hasAndGroup()) return false;
      if (e.getExceptionList() != null) return false;
      if (e.isReferenceElement()) return false;
      if (e.isSentenceStart()) return false;
      
      String token = e.getString();
      String[] ors = token.split("\\|");
      for (String s : ors) {
        if (RuleConverter.isRegex(s)) {
          return false;
        }
      }
      return true;
    }
    
    private static boolean isOrRegex(PatternToken e) {
      if (e.getString().isEmpty()) return false;
      String token = e.getString();
      String[] ors = token.split("\\|");
      for (String s : ors) {
        if (RuleConverter.isRegex(s)) {
          return false;
        }
      }
      return true; 
    }
    
    // ** DICTIONARY METHODS ** 
    
    private DictionaryIterator resetDictIterator() {
        DictionaryIterator ret = null;
        try {
          ret = new DictionaryIterator(Dictionary.read(dictFile), Charset.forName("utf8").newDecoder(), true);
        } catch (IOException e) {
          throw new RuntimeException("Could not read " + dictFile, e);
        }
        return ret;        
    }
    
    private IStemmer loadDictionary() throws IOException {
        IStemmer dictLookup = new DictionaryLookup(Dictionary.read(dictFile));
        return dictLookup;
    }
    
    // try several ways to open the dictionary file
    private void setupDictionaryFiles() {
       try {
         filename = "" +  JLanguageTool.getDataBroker().getResourceDir() + "/" + 
               language.getShortCode() + "/" + language.getName().toLowerCase() + ".dict";
          dictFile = Paths.get(filename);
          dictLookup = (DictionaryLookup) loadDictionary();
          dictIterator = resetDictIterator();
        } catch (IOException e) {
          try {
            // a different formulation of the filename
            filename = "./src/" +  JLanguageTool.getDataBroker().getResourceDir() + "/" + 
              language.getShortCode() + "/" + language.getName().toLowerCase() + ".dict";
            dictFile = Paths.get(filename);
            dictLookup = (DictionaryLookup) loadDictionary();
            dictIterator = resetDictIterator();
          } catch (IOException e2) {
            throw new RuntimeException(e2);
          }
        }
    }
    
    public List<AbstractPatternRule> loadPatternRules(final String filename)
        throws IOException {
      final PatternRuleLoader ruleLoader = new PatternRuleLoader();
      InputStream is = this.getClass().getResourceAsStream(filename);
      if (is == null) {
        // happens for external rules plugged in as an XML file:
        return ruleLoader.getRules(new File(filename));
      } else {
        return ruleLoader.getRules(is, filename);
      }
    }
    
    public List<AbstractPatternRule> parsePatternRule(final String ruleString) {
      final PatternRuleLoader ruleLoader = new PatternRuleLoader();
      String ruleFileString = ruleFileHeader + categoriesString + ruleString + endCategoriesString + endRulesString;
      InputStream is = new ByteArrayInputStream(ruleFileString.getBytes());
      try {
        return ruleLoader.getRules(is, null);
      } catch (IOException e) {
        return new ArrayList<>();
      }
    }
    
    public List<AbstractPatternRule> parsePatternRuleExtraTokens(final String ruleString) {
      String rs = ruleString;
      rs = rs.replace("<pattern>\n", "<pattern>\n<token/>\n");
    rs = rs.replace("</pattern>\n", "<token/>\n</pattern>\n");
      final PatternRuleLoader ruleLoader = new PatternRuleLoader();
      String ruleFileString = ruleFileHeader + categoriesString + rs + endCategoriesString + endRulesString;
      InputStream is = new ByteArrayInputStream(ruleFileString.getBytes());
      try {
        return ruleLoader.getRules(is, null);
      } catch (IOException e) {
        return new ArrayList<>();
      }
    }
    
    public void enableRule(String id) {
      tool.enableRule(id);
    }
    
    
}
