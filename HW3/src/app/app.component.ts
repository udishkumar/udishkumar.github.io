import { Component, OnInit, ChangeDetectorRef, ElementRef, Renderer2} from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { faSearch, faBars } from '@fortawesome/free-solid-svg-icons';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { tap } from 'rxjs/operators';

interface Product {
  product_id: string;
  product_image: string;
  product_name: string;
  product_price: number;
  shippingType: string;
}


interface SimilarItem {
  product_id: string;
  image_url: string;
  product_url: string;
  product_name: string;
  price: string;
  shipping_cost: string;
  daysLeft: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
  isDetailVisible: boolean = false;
  searchForm!: FormGroup;
  faSearch = faSearch;
  faBars = faBars;
  keywordError: string = '';
  zipError: string = '';
  options: string[] = [];
  isLoading: boolean = false;
  noRecord: boolean = false;
  products: any[] = [];
  allProducts: any[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  pages: number[] = [];
  productDetails: any;
  carouselImages: string[] = [];
  isModalOpen: boolean = false;
  isWishlisted: boolean = false;
  productImages: string[] = [];
  productShippingDetails: string[] = [];
  similarItems: SimilarItem[] = [];
  displayedItems: SimilarItem[] = [];
  showMore = false;
  selectedSortOption = 'default';
  selectedOrder = 'asc';
  selectedProduct: any = null;
  wishlistProducts: any[] = [];
  serverUrl: string = 'https://udishkumar-hw3.wl.r.appspot.com';
  matchingProduct: any;


  ngOnInit() {
    this.searchForm = new FormGroup({
      keyword: new FormControl('', Validators.required),
      category: new FormControl('All Categories'),
      condition: new FormGroup({
        new: new FormControl(false),
        used: new FormControl(false),
        unspecified: new FormControl(false)
      }),
      shippingOptions: new FormGroup({
        localPickup: new FormControl(false),
        freeShipping: new FormControl(false)
      }),
      distance: new FormControl(),
      location: new FormControl('current', Validators.required),
      zipCodeOthers: new FormControl(null, Validators.required),
      zipCodeCurrent: new FormControl(null)
    });
    
    this.searchForm.get('keyword')!.valueChanges.subscribe(value => {
      const keywordControl = this.searchForm.get('keyword');
      
      if (keywordControl && keywordControl.invalid && (value === null || value.trim() === '') && (keywordControl.dirty || keywordControl.touched)) {
        this.keywordError = 'Please enter a keyword.';
        this.cd.detectChanges();
      } else {
        this.keywordError = '';
      }
    });    

    this.searchForm.get('location')!.valueChanges.subscribe(value => {
      const zipCodeControl = this.searchForm.get('zipCodeOthers');
      if (value === 'current') {
        this.fetchCurrentLocationPostal().subscribe(postalCode => {
          this.searchForm.get('zipCodeCurrent')?.setValue(postalCode);
        });
    
        zipCodeControl?.setValue(null);
        zipCodeControl?.clearValidators();
    
      } else if (value === 'other') {
        this.searchForm.get('zipCodeCurrent')?.setValue(null);
        zipCodeControl?.setValidators(Validators.required);
      }
    
      zipCodeControl?.updateValueAndValidity();
    });
    
    
    
    console.log('Check validation for zip code and update the error message');
    this.searchForm.get('zipCodeOthers')!.valueChanges.subscribe(value => {
      if (this.searchForm.get('location')!.value === 'other' && value && (!/^\d{5}$/.test(value.trim()))) {
        this.zipError = 'Please enter a valid 5-digit zip code.';
      } else {
        this.zipError = '';
      }
    });
    this.cd.detectChanges();

    this.searchForm.get('zipCodeOthers')?.valueChanges
      .pipe(
        debounceTime(300),             
        distinctUntilChanged(),       
        switchMap(query => this.fetchPostalCodes(query)),
        catchError(err => {
          this.zipError = 'Error fetching postal codes';
          return of([]);
        })
      )
      .subscribe(postalCodes => {
        this.options = postalCodes;
      });

    const locationValue = this.searchForm.get('location')!.value;

    if (locationValue === 'current') {
        this.fetchCurrentLocationPostal().subscribe(postalCode => {
          this.searchForm.get('zipCodeCurrent')?.setValue(postalCode);
        });
    }
    this.showWishlist();
  }

  constructor(private cd: ChangeDetectorRef, private http: HttpClient, private renderer: Renderer2, private el: ElementRef) { } // Inject ChangeDetectorRef here


  private removeBackdrop() {
    const backdrops: NodeListOf<HTMLElement> = document.querySelectorAll('.modal-backdrop');
    backdrops.forEach(backdrop => backdrop.remove());
  }

  isFormValid(): boolean {
    const keywordValue = this.searchForm.get('keyword')!.value?.trim();
    const locationValue = this.searchForm.get('location')!.value;
    const zipValue = this.searchForm.get('zipCodeOthers')!.value?.trim();

    if (!keywordValue && locationValue === 'current') {
        return false;
    }

    if (locationValue === 'current') {
        return keywordValue !== '';
    }

    if (locationValue === 'other') {
        if (!zipValue) {
            return false;
        }
        return /^\d{5}$/.test(zipValue);
    }

    return keywordValue !== '';
}

toggleWishlistIcon(product: any, wishlistproduct_id: any) {
  console.log("wishlist icon clicked");
  if(!product) {
    this.removeFromWishlist(wishlistproduct_id).subscribe(() => {
      this.showWishlist();
    });
  }
  else{
    if (product.wishlistIcon === 'icon1.svg') {
      product.wishlistIcon = 'icon2.svg';
      this.addToWishlist(product).subscribe(() => {
        this.showWishlist();
      });
    } else {
      product.wishlistIcon = 'icon1.svg';
      this.removeFromWishlist(product.product_id).subscribe(() => {
        this.showWishlist();
      });
    }
  }
 
}

getWishlistIcon(wishlistProductId: string): string {
  if (!wishlistProductId) {
    return 'assets/svg/icon2.svg';
  }
  const product = this.allProducts.find(product => product.product_id === wishlistProductId);
  return product ? `assets/svg/${product.wishlistIcon}` : 'assets/svg/icon2.svg';
}

getWishlistProduct(wishlistProductId: string): Product | null {
  return this.allProducts.find(product => product.product_id === wishlistProductId) || null;
}

reloadWishlist() {
  console.log("wish list updated");
  this.showWishlist();
  this.wishlistProducts = [...this.wishlistProducts];
  console.log(JSON.stringify(this.wishlistProducts));
}

removeFromWishlist(product_id: any): Observable<any> {
  return this.http.delete(`${this.serverUrl}/wishlist/${product_id}`)
    .pipe(
      tap(response => console.log('Removed from wishlist:', response))
    );
}

addToWishlist(product: any): Observable<any> {
  const body = {
      product_id: product.product_id,
      product_image: product.product_image,
      product_name: product.product_name,
      product_price: product.product_price,
      shippingType: product.shippingType,
  };
  
  return this.http.post(`${this.serverUrl}/wishlist`, body)
    .pipe(
      tap(response => console.log('Added to wishlist:', response))
    );
}


showWishlist() {
  this.http.get<Product[]>(`${this.serverUrl}/wishlist`).subscribe((data: Product[]) => {
    this.wishlistProducts = data;
});
}

setupPagination(products: any[]) {
  let productIndex = 1;
  products.forEach(product => {
    product.index = productIndex++;
    const productFromWL = this.wishlistProducts.find(wishlistProduct => wishlistProduct.product_id ===  product.product_id);
    if(productFromWL){
      product.wishlistIcon = 'icon2.svg';
    }else{
      product.wishlistIcon = 'icon1.svg';
    }
  });

  this.allProducts = products;

  const totalPages = Math.ceil(products.length / this.itemsPerPage);
  this.pages = Array(totalPages).fill(0).map((x, i) => i + 1);

  this.displayProductsForPage(1); 
}

openImageCarousel(images: string[]) {
  this.carouselImages = images;
  this.isModalOpen = true;
  document.body.classList.add('modal-open');
}

closeModal() {
  this.isModalOpen = false;
  document.body.classList.remove('modal-open');
  this.removeBackdrop();
}

backToProductsList() {
  this.isDetailVisible = true;
  this.productDetails = null;
}

selectProductFromWishlist(wishlistProductId: string) {
  const matchingProduct = this.products.find(product => product.product_id === wishlistProductId);
  if (matchingProduct) {
    this.selectedProduct = matchingProduct;
  } else {
    console.error('Matching product not found in products list');
  }
}


viewProductDetails(product: any) {
  console.log(this.matchingProduct+' matching product entered in viewProductDetails method '+JSON.stringify(product));
    this.isDetailVisible = false;
    this.isLoading = true;
    this.productDetails = null;
    this.selectedProduct = product;
    const productId = product.product_id;

  this.http.get(`${this.serverUrl}/getEbayData/${productId}`).subscribe((response: any) => {
      this.isLoading = false;
      const colorToHexMapping: { [key: string]: string } = {
        'Yellow': '#FFFF00',
        'Blue': '#0000FF',
        'Turquoise': '#40E0D0',
        'Purple': '#800080',
        'Red': '#FF0000',
        'Green': '#008000',
        'Silver': '#C0C0C0'
    };

    const extractColorFromValue = (value: string): string => {
      const color = value.replace("Shooting", "");
      return colorToHexMapping[color] || '';
    }
      this.similarItems = response.similarItems;
      this.displayedItems = this.similarItems.slice(0, this.showMore ? 20 : 5);
      if(product.shipping_cost != ''){
        response.shipping_cost = '$'+product.shipping_cost;
      }
      else{response.shipping_cost = product.shippingType+' Shipping';}
      response.shipping_locations = product.shipping_locations;
      response.handling_time = product.handling_time;
      response.feedback_score = product.feedback_score;
      response.popularity = product.popularity;
      response.feedback_rating_star = extractColorFromValue(product.feedback_rating_star);
      response.store_name = product.store_name;
      response.buy_product_at = product.buy_product_at;
      response.seller = product.seller;
      response.product_url = encodeURIComponent(response.product_url);
      if (product.expedited_shipping == 'true') {
        response.expedited_shipping = true;
      }
    
      if (product.one_day_shipping == 'true') {
          response.one_day_shipping = true;
      }
      
      if (product.return_accepted == 'true') {
          response.return_accepted = true;
      }
      if (product.top_rated == 'true') {
          response.top_rated = true;
      }
      
      console.log('Product Details:', response);
      
      this.productDetails = response;
      const productName = encodeURIComponent(response.product_name);
      this.http.get(`${this.serverUrl}/getPhotos/${productName}`).subscribe((imgResponse: any) => {
        this.productImages = [
          imgResponse.link1, imgResponse.link2, imgResponse.link3, imgResponse.link4,
          imgResponse.link5, imgResponse.link6, imgResponse.link7, imgResponse.link8
        ];
      });
  });
}

wishlistViewProductDetails(wishlistProductId: string) {
  console.log('I am clicked from wishlistViewProductDetails method' + wishlistProductId);
  this.matchingProduct = this.allProducts.find(product => product.product_id === wishlistProductId);
  console.log(wishlistProductId+' ID matched with '+this.matchingProduct.product_id);
  if (this.matchingProduct) {
    this.viewProductDetails(this.matchingProduct);
  } else {
    console.error('Product not found in the main product list');
  }
}

displayProductsForPage(page: number) {
    const startIndex = (page - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.products = this.allProducts.slice(startIndex, endIndex);
    this.currentPage = page;
}

previousPage(event: Event) {
  event.preventDefault();
  if(this.currentPage > 1) {
    this.currentPage--;
    this.displayProductsForPage(this.currentPage);
  }
}

nextPage(event: Event) {
  event.preventDefault();
  if(this.currentPage < this.pages.length) {
    this.currentPage++;
    this.displayProductsForPage(this.currentPage);
  }
}

goToPage(page: number, event: Event) {
  event.preventDefault();
  this.displayProductsForPage(page);
}

getTotalPrice(): number {
  const sum = this.wishlistProducts.reduce((total, product) => total + parseFloat(product.product_price), 0);
  return parseFloat(sum.toFixed(2));
}

fetchPostalCodes(query: string): Observable<string[]> {
  if (!query || query.length < 1) {
    return of([]);
  }
  return this.http.get<string[]>(`${this.serverUrl}/fetchPostalCodes?zipCodeStartsWith=${query}`);
}

onOptionSelected(event: MatAutocompleteSelectedEvent): void {
  const selectedValue = event.option.value;
  this.searchForm.get('zipCodeOthers')?.setValue(selectedValue);
}

onSubmit() {
  this.isDetailVisible=true;
  this.productDetails = null;
  this.products = [];   
  this.allProducts = [];   
  this.pages = [];          
  this.selectedSortOption = 'default';
  const requestData = this.generateRequestData();
  console.log('Request Data:', requestData);
  this.isLoading = true;

  setTimeout(() => {
    this.sendJSONData(requestData).subscribe(response => {
      if(response.length > 0){
        console.log('API Response:', response);
        this.setupPagination(response);
        this.isLoading = false;
      }else{
        this.isLoading = false;
        this.noRecord = true;
      }
      
  }, error => {
      console.error('Error from API:', error);
      this.isLoading = false;
  });
}, 1000);
}

sendJSONData(data: any): Observable<any> {
  const endpoint = `${this.serverUrl}/getEbayData`;
  
  const params = new HttpParams({ fromObject: data });
  
  return this.http.get(endpoint, { params });
}

onReset() {
  this.searchForm.reset({
    category: 'All Categories',
    location: 'current',
    zipCodeOthers: null
  });
  this.keywordError = '';
}

preventNegativeInput(event: KeyboardEvent): void {
  if (event.key === '-' || event.keyCode === 189) { 
    event.preventDefault();
  }
}

fetchCurrentLocationPostal(): Observable<string> {
  return this.http.get<any>('https://ipinfo.io/json?token=ea42c97831bf33')
    .pipe(
      map(data => data.postal),
      catchError(error => {
        console.error("Failed to fetch postal code from IP info:", error);
        return of('');
      })
    );
}

clearForm(): void {
  this.searchForm.reset();
  this.noRecord = false;
  this.searchForm.patchValue({
    keyword: '',
    category: 'All Categories',
    condition: {
      new: false,
      used: false,
      unspecified: false
    },
    shippingOptions: {
      localPickup: false,
      freeShipping: false
    },
    distance: null,
    location: 'current',
    zipCodeOthers: null
  });

  
  this.products = [];      
  this.allProducts = [];  
  this.pages = [];         
  this.currentPage = 1;     
  this.productDetails = null;
  this.selectedProduct = null;
  this.selectedSortOption = 'default';
  this.isDetailVisible = false;
}

generateRequestData(): any {
  const formValues = this.searchForm.value;
  const categoryMapping: { [key: string]: number } = {
    'Art': 550,
    'Baby': 2984,
    'Books': 267,
    'Clothing, Shoes & Accessories': 11450,
    'Computers/Tablets & Networking': 58058,
    'Health & Beauty': 26395,
    'Music': 11233,
    'Video Games & Consoles': 1249
  };
  let conditionValue: string = "";
  const conditions = formValues.condition;
  if (conditions.new && conditions.used) {
    conditionValue = "1000,3000";
  } else if (conditions.new) {
    conditionValue = "1000";
  } else if (conditions.used) {
    conditionValue = "3000";
  } else if (conditions.unspecified && conditions.new){
    conditionValue = "1000";
  } else if (conditions.unspecified && conditions.used){
    conditionValue = "3000";
  } else if (conditions.unspecified && conditions.used && conditions.new){
    conditionValue = "1000,3000";
  }

  const shippingOptions = formValues.shippingOptions;
  const localPickupOnly = shippingOptions.localPickup || false;
  const freeShippingOnly = shippingOptions.freeShipping || false;
  const requestData: any = {
    condition: conditionValue,
    keywords: formValues.keyword,
    maxDistance: formValues.distance || "",
    localPickupOnly: localPickupOnly,
    freeShippingOnly: freeShippingOnly,
    zipCode: formValues.location === 'current' ? formValues.zipCodeCurrent : formValues.zipCodeOthers
  };
  if (formValues.category !== 'All Categories') {
    requestData.category = categoryMapping[formValues.category];
  }

  return requestData;
}

toggleItems(): void {
  this.showMore = !this.showMore;
  const itemsToShow = this.showMore ? 20 : 5;
  this.displayedItems = this.similarItems.slice(0, itemsToShow);
}

sortItems(): void {
  if (this.selectedSortOption === 'default') {
    this.displayedItems = this.similarItems.slice(0, this.showMore ? this.similarItems.length : 5);
    return;
  }

  this.similarItems.sort((a: SimilarItem, b: SimilarItem) => {
    let comparison = 0;

    switch (this.selectedSortOption) {
      case 'productName':
        comparison = a.product_name.toLowerCase().localeCompare(b.product_name.toLowerCase());
        break;
      
      case 'daysLeft':
        comparison = parseInt(a.daysLeft) - parseInt(b.daysLeft);
        break;

      case 'price':
        comparison = parseFloat(a.price) - parseFloat(b.price);
        break;

      case 'shippingCost':
        comparison = parseFloat(a.shipping_cost) - parseFloat(b.shipping_cost);
        break;

      default:
        break;
    }
    if (this.selectedOrder === 'desc') {
      comparison = -comparison;
    }

    return comparison;
  });
  this.displayedItems = this.similarItems.slice(0, this.showMore ? this.similarItems.length : 5);
}


}
