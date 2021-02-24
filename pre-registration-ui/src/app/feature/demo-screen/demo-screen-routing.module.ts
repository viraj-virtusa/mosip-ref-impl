import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DemoScreenComponent } from './demo-screen/demo-screen.component';
const routes: Routes = [{ path: '', component: DemoScreenComponent }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DemoScreenRoutingModule {}
