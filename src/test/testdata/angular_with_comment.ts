import { Component } from '@angular/core';

// selector: 'commented-out'
/* selector: 'also-commented' */

@Component({
  selector: 'app-with-comment',
  template: '<div>Test</div>'
})
export class WithCommentComponent {
  // This is a property
  title = 'Test Component';
}
