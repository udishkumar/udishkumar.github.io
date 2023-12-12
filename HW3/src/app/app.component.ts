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
  // serverUrl: string = 'http://localhost:8080';
  serverUrl: string = 'https://uk-hw3.wl.r.appspot.com';
  matchingProduct: any;
  wishlistActive: boolean = false;
  resultSectionActive: boolean = true;
  activeTab: string = 'results';
  currentProduct: any;


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
    
    // Check validation whenever the keyword input changes
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
        // Fetch the current location postal code
        this.fetchCurrentLocationPostal().subscribe(postalCode => {
          this.searchForm.get('zipCodeCurrent')?.setValue(postalCode);
        });
    
        // Clear the value of zipCodeOthers and remove its validators
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

    // If the default location value is 'current', fetch the postal code
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

  // This method returns true if the form is valid, otherwise false.
  isFormValid(): boolean {
    const keywordValue = this.searchForm.get('keyword')!.value?.trim();
    const locationValue = this.searchForm.get('location')!.value;
    const zipValue = this.searchForm.get('zipCodeOthers')!.value?.trim();

    if (!keywordValue && locationValue === 'current') {
        return false;
    }

    // If location is 'current', validate only keyword
    if (locationValue === 'current') {
        return keywordValue !== '';
    }

    // If location is 'other', validate only zipCode
    if (locationValue === 'other') {
        if (!zipValue) {
            return false;
        }
        return /^\d{5}$/.test(zipValue);
    }

    // Default case (when the form loads)
    return keywordValue !== '';
}

toggleWishlistIcon(product: any, wishlistproduct_id: any) {
  console.log("wishlist icon clicked");
  // console.log(JSON.stringify(product));
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
      shippingType: product.shippingType
  };
  
  return this.http.post(`${this.serverUrl}/wishlist`, body)
    .pipe(
      tap(response => console.log('Added to wishlist:', response))
    );
}

// When the wishlist tab is clicked, fetch the wishlist items
showWishlist() {
  this.http.get<Product[]>(`${this.serverUrl}/wishlist`).subscribe((data: Product[]) => {
    this.wishlistProducts = data;
});
}

setActiveTab(tabName: string) {
  this.activeTab = tabName;
  if (this.activeTab == 'results'){
    this.wishlistActive = false;
    this.resultSectionActive = true;
    console.log("cuurent tab is-", this.activeTab);
    console.log("wishlistactive is-", this.wishlistActive);
    console.log("resultSectionActive is-", this.resultSectionActive);
  }else{
    this.wishlistActive = true;
    this.resultSectionActive = false;
    this.showWishlist();
    console.log("cuurent tab is-", this.activeTab);
    console.log("wishlistactive is-", this.wishlistActive);
    console.log("resultSectionActive is-", this.resultSectionActive);
  }
}


// Call this function after getting API response
setupPagination(products: any[]) {
  // Assign index and wishlistIcon properties to each product
  let productIndex = 1;
  products.forEach(product => {
    product.index = productIndex++;
    // product.wishlistIcon = 'icon1.svg';  // By default, setting the icon to 'icon1.svg'
    const productFromWL = this.wishlistProducts.find(wishlistProduct => wishlistProduct.product_id ===  product.product_id);
    // console.log(this.wishlistProducts);
    if(productFromWL){
      product.wishlistIcon = 'icon2.svg';
    }else{
      product.wishlistIcon = 'icon1.svg';
    }
    // return product ? `assets/svg/${product.wishlistIcon}` : 'assets/svg/icon2.svg';
    // const isWishlisted = this.wishlistProducts.some(wishlistProduct => wishlistProduct.product_id === product.product_id);
    // console.log("exists in wishilist",product.product_id, product.wishlistIcon);
    // product.wishlistIcon = isWishlisted ? 'icon2.svg' : 'icon1.svg';
  });

  this.allProducts = products;  // Storing the entire list of products

  const totalPages = Math.ceil(products.length / this.itemsPerPage);
  this.pages = Array(totalPages).fill(0).map((x, i) => i + 1);

  this.displayProductsForPage(1);  // Displaying the first page of products initially
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
  // Find the product in the products array that matches the wishlist product ID
  const matchingProduct = this.products.find(product => product.product_id === wishlistProductId);
  
  // If there's a matching product, set it as the selectedProduct
  if (matchingProduct) {
    this.selectedProduct = matchingProduct;
  } else {
    console.error('Matching product not found in products list');
  }
}


viewProductDetailsForResult(product: any) {

  // this.currentProduct = this.allProducts.find(product => product.product_id === product_id);
  console.log(this.matchingProduct+' matching product entered in viewProductDetails method ',product);
    this.isDetailVisible = false; // Hide the Detail button
    this.isLoading = true; // Show the progress bar
    this.productDetails = null; // Reset product details
    this.selectedProduct = product;
    const productId = product.product_id;
  console.log("selected product is-", this.selectedProduct);

  // Make the API call
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
      // Remove "Shooting" suffix to get the actual color
      const color = value.replace("Shooting", "");
      return colorToHexMapping[color] || ''; // If color doesn't exist in the map, return an empty string
    }
      this.similarItems = response.similarItems;
      // In the subscription callback, after you've set similarItems:
      this.displayedItems = this.similarItems.slice(0, this.showMore ? 20 : 5);

      // this.displayedItems = this.similarItems.slice(0, 5);
      // Add the fields to the response
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
      // Now assign the updated response to productDetails
      
      this.productDetails = response;
      // Extract product_name and make a secondary API call to get image URLs
      const productName = encodeURIComponent(response.product_name);
      this.http.get(`${this.serverUrl}/getPhotos/${productName}`).subscribe((imgResponse: any) => {
        this.productImages = [
          imgResponse.link1, imgResponse.link2, imgResponse.link3, imgResponse.link4,
          imgResponse.link5, imgResponse.link6, imgResponse.link7, imgResponse.link8
        ];
      });
  });
}

viewProductDetailsForWishList(product: any) {
  // this.currentProduct = this.allProducts.find(product => product.product_id === product_id);
  console.log(this.matchingProduct+' matching product entered in viewProductDetails method '+JSON.stringify(product));
    this.isDetailVisible = false; // Hide the Detail button
    this.isLoading = true; // Show the progress bar
    this.productDetails = null; // Reset product details
    this.selectedProduct = product;
    const productId = product.product_id;
  
  // Make the API call
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
      // Remove "Shooting" suffix to get the actual color
      const color = value.replace("Shooting", "");
      return colorToHexMapping[color] || ''; // If color doesn't exist in the map, return an empty string
    }
      this.similarItems = response.similarItems;
      // In the subscription callback, after you've set similarItems:
      this.displayedItems = this.similarItems.slice(0, this.showMore ? 20 : 5);

      // this.displayedItems = this.similarItems.slice(0, 5);
      // Add the fields to the response
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
      // Now assign the updated response to productDetails
      
      this.productDetails = response;
      // Extract product_name and make a secondary API call to get image URLs
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
  // Find the product in the main products array that matches the wishlist product ID
  const productFromWL = this.wishlistProducts.find(wishlistProduct => wishlistProduct.product_id ===  wishlistProductId);
  console.log("wishlist product clicked ----",productFromWL)
  this.matchingProduct = this.allProducts.find(product => product.product_id === wishlistProductId);
  console.log(wishlistProductId+' ID matched with '+this.matchingProduct.product_id);
  // If there's a matching product in the main product list, call viewProductDetails
  if (this.matchingProduct) {
    this.viewProductDetailsForResult(this.matchingProduct);
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
  this.productDetails = null; // Reset product details
  this.products = [];        // Clearing the currently displayed products
  this.allProducts = [];     // Clearing all products
  this.pages = [];           // Clearing pagination pages
  this.selectedSortOption = 'default';
  const requestData = this.generateRequestData();
  console.log('Request Data:', requestData);
  // Show the progress bar
  this.isLoading = true;

  // Delay for 2 seconds before making the API call
  setTimeout(() => {
    this.sendJSONData(requestData).subscribe(response => {
      if(response.length > 0){
        console.log('API Response:', response);
        this.setupPagination(response);  // Setup the pagination and products
        this.isLoading = false;
        this.noRecord = false;
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
  // Construct the base endpoint URL
  const endpoint = `${this.serverUrl}/getEbayData`;
  
  // Convert the data object to query parameters
  const params = new HttpParams({ fromObject: data });
  
  return this.http.get(endpoint, { params });
}

onReset() {
  this.searchForm.reset({
    category: 'All Categories',
    location: 'current',
    zipCodeOthers: null
  });
  // Resetting to default values for category and location, 
  this.keywordError = ''; // Resetting the error message
}

preventNegativeInput(event: KeyboardEvent): void {
  if (event.key === '-' || event.keyCode === 189) { // keyCode 189 is for the minus key
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
  // 1. Reset the form to its initial state
  this.searchForm.reset();
  this.noRecord = false;
  // 2. Set default or empty values for form fields
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

  // 3. Clear the table and pagination related view
  this.products = [];        // Clearing the currently displayed products
  this.allProducts = [];     // Clearing all products
  this.pages = [];           // Clearing pagination pages
  this.currentPage = 1;      // Resetting the current page to 1
  
  // 4. Close the product details view
  this.productDetails = null; // This will ensure the product details view is not displayed
  this.selectedProduct = null;
  this.selectedSortOption = 'default';
  this.isDetailVisible = false;
}

generateRequestData(): any {
  // Extract form values
  const formValues = this.searchForm.value;

  // 1. Category mapping
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

  // 2. Determine condition value
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

  // 3. Determine shipping option values
  const shippingOptions = formValues.shippingOptions;
  const localPickupOnly = shippingOptions.localPickup || false;
  const freeShippingOnly = shippingOptions.freeShipping || false;

  // 4. Structure the JSON response
  const requestData: any = {
    condition: conditionValue,
    keywords: formValues.keyword,
    maxDistance: formValues.distance || "",
    localPickupOnly: localPickupOnly,
    freeShippingOnly: freeShippingOnly,
    zipCode: formValues.location === 'current' ? formValues.zipCodeCurrent : formValues.zipCodeOthers
  };

  // Conditionally add category if not "All Categories"
  if (formValues.category !== 'All Categories') {
    requestData.category = categoryMapping[formValues.category];
  }

  return requestData;
}

// Function to toggle between showing more/less similar items
toggleItems(): void {
  // Toggle the state
  this.showMore = !this.showMore;

  // Adjust the displayed items based on the updated state
  const itemsToShow = this.showMore ? 20 : 5;
  this.displayedItems = this.similarItems.slice(0, itemsToShow);
}

// Function to sort the similar items
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

    // If descending is selected, reverse the order
    if (this.selectedOrder === 'desc') {
      comparison = -comparison;
    }

    return comparison;
  });

  // Update displayed items based on whether showMore is true or false
  this.displayedItems = this.similarItems.slice(0, this.showMore ? this.similarItems.length : 5);
}


}
