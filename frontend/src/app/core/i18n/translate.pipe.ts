import { ChangeDetectorRef, inject, Pipe, PipeTransform } from '@angular/core';

import { TranslationKey } from './i18n.model';
import { I18nService } from './i18n.service';

@Pipe({
  name: 'translate',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  private readonly i18nService = inject(I18nService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  transform(key: TranslationKey): string {
    this.i18nService.locale();
    this.changeDetectorRef.markForCheck();
    return this.i18nService.translate(key);
  }
}

