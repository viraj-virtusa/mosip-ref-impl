import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DemoScreenComponent } from './demo-screen/demo-screen.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { DemoScreenRoutingModule } from './demo-screen-routing.module';

@NgModule({
  declarations: [DemoScreenComponent],
  imports: [CommonModule, SharedModule, DemoScreenRoutingModule]
})
export class DemoScreenModule { }
