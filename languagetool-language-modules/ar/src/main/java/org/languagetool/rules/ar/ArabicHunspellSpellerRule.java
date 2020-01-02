/* LanguageTool, a natural language style checker
 * Copyright (C) 2019 Sohaib Afifi, Taha Zerrouki
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

package org.languagetool.rules.ar;

import org.jetbrains.annotations.NotNull;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.language.Arabic;
import org.languagetool.rules.spelling.hunspell.HunspellRule;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @since 4.9
 */
public final class ArabicHunspellSpellerRule extends HunspellRule {

  public static final String RULE_ID = "HUNSPELL_RULE_AR";
  
  private static final String RESOURCE_FILENAME = "/ar/hunspell/ar.dic";

  public ArabicHunspellSpellerRule(ResourceBundle messages, Language language, UserConfig userConfig) {
    super(messages, language, userConfig);
  }

  public ArabicHunspellSpellerRule(ResourceBundle messages, UserConfig userConfig) {
    this(messages, new Arabic(), userConfig);
  }

  public ArabicHunspellSpellerRule(ResourceBundle messages) {
    this(messages, null);
  }

  public ArabicHunspellSpellerRule(ResourceBundle messages, Language language, UserConfig userConfig, List<Language> altLanguages) {
    super(messages, language, userConfig, altLanguages);
  }

  @Override
  public String getId() {
    return RULE_ID;
  }

  @Override
  @NotNull
  protected String getDictFilenameInResources(String langCountry) {
    return RESOURCE_FILENAME;
  }
}
