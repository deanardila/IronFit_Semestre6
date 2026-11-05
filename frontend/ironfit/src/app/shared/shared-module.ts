import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MiPerfil } from '../modules/cliente/pages/mi-perfil/mi-perfil';
import { IronBotPanel } from './ironbot-panel';

@NgModule({
    declarations: [MiPerfil, IronBotPanel],
    imports: [CommonModule, FormsModule],
    exports: [MiPerfil, IronBotPanel],
})
export class SharedModule {}
