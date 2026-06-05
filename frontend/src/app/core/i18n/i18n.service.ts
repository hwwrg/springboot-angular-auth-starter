import { computed, Injectable, signal } from '@angular/core';

import { EN_TRANSLATIONS, FR_TRANSLATIONS, ZH_TRANSLATIONS } from './translations';
import { LocaleCode, TranslationDictionary, TranslationKey } from './i18n.model';

const DICTIONARIES: Record<LocaleCode, TranslationDictionary> = {
  en: EN_TRANSLATIONS,
  fr: FR_TRANSLATIONS,
  zh: ZH_TRANSLATIONS,
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly currentLocaleSignal = signal<LocaleCode>('en');

  readonly locale = this.currentLocaleSignal.asReadonly();
  readonly locales = ['en', 'fr', 'zh'] as const;
  readonly dictionary = computed(() => DICTIONARIES[this.currentLocaleSignal()]);

  setLocale(locale: LocaleCode): void {
    this.currentLocaleSignal.set(locale);
  }

  translate(key: TranslationKey): string {
    return this.dictionary()[key];
  }
}

