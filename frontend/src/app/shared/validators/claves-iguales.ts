import { ErrorStateMatcher } from '@angular/material/core';
import {
  AbstractControl,
  FormControl,
  FormGroupDirective,
  NgForm,
  ValidationErrors,
} from '@angular/forms';

/** Ambos campos de clave deben coincidir; el error se muestra en el campo de repetición. */
export function clavesIguales(group: AbstractControl): ValidationErrors | null {
  const clave = group.get('clave')?.value;
  const repetir = group.get('repetirClave')?.value;
  return clave === repetir ? null : { clavesDistintas: true };
}

/**
 * El error de validación cruzada vive en el FormGroup, pero MatFormField solo
 * consulta el control del campo. Este matcher hace visible el error del grupo
 * en el campo "Repetir contraseña".
 */
export class ClavesIgualesErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const touched = !!control && (control.touched || control.dirty);
    const submitted = !!form?.submitted;
    const groupError = !!form?.form?.hasError('clavesDistintas');
    return (touched || submitted) && (!!control?.invalid || groupError);
  }
}
