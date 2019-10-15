/* LanguageTool, a natural language style checker 
 * Copyright (C) 2009 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.language;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.Language;
import org.languagetool.LanguageMaintainedState;
import org.languagetool.UserConfig;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.rules.*;
import org.languagetool.rules.ga.*;
import org.languagetool.synthesis.Synthesizer;
import org.languagetool.synthesis.ga.IrishSynthesizer;
import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.ga.IrishTagger;
import org.languagetool.tagging.disambiguation.Disambiguator;
import org.languagetool.tagging.disambiguation.ga.IrishHybridDisambiguator;
import org.languagetool.tokenizers.SRXSentenceTokenizer;
import org.languagetool.tokenizers.SentenceTokenizer;
import org.languagetool.tokenizers.Tokenizer;
import org.languagetool.tokenizers.WordTokenizer;

public class Irish extends Language implements AutoCloseable {

  private static final Language DEFAULT_IRISH = new Irish();
  
  private Tagger tagger;
  private SentenceTokenizer sentenceTokenizer;
  private Tokenizer wordTokenizer;
  private Synthesizer synthesizer;
  private Disambiguator disambiguator;
  private LanguageModel languageModel;

  @Override
  public String getName() {
    return "Irish";
  }

  @Override
  public String[] getCountries() {
    return new String[]{"IE"};
  }
  
  @Override
  public String getShortCode() {
    return "ga";
  }

  @Override
  public Language getDefaultLanguageVariant() {
    return DEFAULT_IRISH;
  }
  
  @Override
  public Contributor[] getMaintainers() {
    return new Contributor[] {
      new Contributor("Jim O'Regan"),
      new Contributor("Emily Barnes"),
      new Contributor("Mícheál J. Ó Meachair"),
      new Contributor("Seanán Ó Coistín")
    };
  }

  @Override
  public List<Rule> getRelevantRules(ResourceBundle messages, UserConfig userConfig, Language motherTongue, List<Language> altLanguages) throws IOException {
    return Arrays.asList(
            new CommaWhitespaceRule(messages),
	    new GenericUnpairedBracketsRule(messages,
                    Arrays.asList("[", "(", "{", "\"", "“"),
                    Arrays.asList("]", ")", "}", "\"", "”")),
            new DoublePunctuationRule(messages),
            new UppercaseSentenceStartRule(messages, this),
	    new LongSentenceRule(messages, userConfig, -1, true),
	    new LongParagraphRule(messages, this, userConfig),
	    new UppercaseSentenceStartRule(messages, this),
            new MultipleWhitespaceRule(messages, this),
	    new SentenceWhitespaceRule(messages),
	    new WhiteSpaceBeforeParagraphEnd(messages, this),
	    new WhiteSpaceAtBeginOfParagraph(messages),
	    new ParagraphRepeatBeginningRule(messages, this),
	    new WordRepeatRule(messages, this),
	    new MorfologikIrishSpellerRule(messages, this, userConfig),
	    new LogainmRule(messages),
	    new PeopleRule(messages),
	    new SpacesRule(messages),
	    new CompoundRule(messages)
    );
  }

  @Override
  public Tagger getTagger() {
    if (tagger == null) {
      tagger = new IrishTagger();
    }
    return tagger;
  }

  @Override
  public Synthesizer getSynthesizer() {
    if (synthesizer == null) {
      synthesizer = new IrishSynthesizer(this);
    }
    return synthesizer;
  }

  @Override
  public SentenceTokenizer getSentenceTokenizer() {
    if (sentenceTokenizer == null) {
      sentenceTokenizer = new SRXSentenceTokenizer(this);
    }
    return sentenceTokenizer;
  }
  
  @Override
  public Disambiguator getDisambiguator() {
    if (disambiguator == null) {
      disambiguator = new IrishHybridDisambiguator();
    }
    return disambiguator;
  }  
  
  @Override
  public Tokenizer getWordTokenizer() {
    if (wordTokenizer == null) {
      wordTokenizer = new WordTokenizer();
    }
    return wordTokenizer;
  }

  @Override
  public LanguageMaintainedState getMaintainedState() {
    return LanguageMaintainedState.ActivelyMaintained;
  }

  @Override
  public synchronized LanguageModel getLanguageModel(File indexDir) throws IOException {
    languageModel = initLanguageModel(indexDir, languageModel);
    return languageModel;
  }

  @Override
  public void close() throws Exception {
    if (languageModel != null) {
      languageModel.close();
    }
  } 
}
