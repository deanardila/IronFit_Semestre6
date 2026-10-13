import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MiPerfil } from '../modules/cliente/pages/mi-perfil/mi-perfil';

@NgModule({
    declarations: [MiPerfil],
    imports: [CommonModule, FormsModule],
    exports: [MiPerfil],
})
export class SharedModule {}
